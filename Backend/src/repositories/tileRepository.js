const db = require('../config/database');

const getAllMasterTiles = async () => {
    // Dipanggil untuk memory cache agar logic validation cepat
    const query = `SELECT * FROM tiles ORDER BY id ASC;`;
    const result = await db.query(query);
    return result.rows;
};

const setupGameTiles = async (gameId) => {
    // Ambil 106 id tiles, masukan ke game_tiles location='POOL'
    const query = `
        INSERT INTO game_tiles (game_id, tile_id, location)
        SELECT $1, id, 'POOL' FROM tiles
    `;
    await db.query(query, [gameId]);
};

const drawTileFromPool = async (gameId, participantId) => {
    // Ambil 1 random ubin dari pool dan assign ke rack participant
    const query = `
        UPDATE game_tiles
        SET location = 'RACK', participant_id = $2
        WHERE id = (
            SELECT id FROM game_tiles
            WHERE game_id = $1 AND location = 'POOL'
            ORDER BY RANDOM()
            LIMIT 1
            FOR UPDATE SKIP LOCKED
        )
        RETURNING tile_id;
    `;
    const result = await db.query(query, [gameId, participantId]);
    return result.rows[0];
};

const getBoardAndRackState = async (gameId, participantId) => {
    // Mengambil state di mana tiles berada ('TABLE' atau 'RACK' milik participant)
    const query = `
        SELECT gt.id AS game_tile_id, gt.tile_id, gt.location, gt.table_set_id,
               ts.set_type
        FROM game_tiles gt
        LEFT JOIN table_sets ts ON gt.table_set_id = ts.id
        WHERE gt.game_id = $1 AND (gt.location = 'TABLE' OR (gt.location = 'RACK' AND gt.participant_id = $2))
    `;
    const result = await db.query(query, [gameId, participantId]);
    return result.rows;
};

// TRANSACTION: Menerapkan state baru dari frontend ke DB jika valid
const applyTurnStateTransaction = async (gameId, participantId, newTableSets, newRackTileIds) => {
    const client = await db.getClient();
    try {
        await client.query('BEGIN');

        // 1. Hapus semua table_sets lama di game ini (game_tiles.table_set_id akan jadi NULL karena ON DELETE SET NULL, 
        // namun locationnya masih 'TABLE', kita perbaiki di bawah)
        await client.query('DELETE FROM table_sets WHERE game_id = $1', [gameId]);

        // Reset location ubin participant di RACK menjadi POOL (sementara untuk validasi query constraint)
        // Sebenarnya lebih aman mereset semua ubin yang terlibat ('TABLE' atau 'RACK' user ini) 
        // ke status mengambang agar tidak melanggar constraints, lalu re-assign.
        await client.query(`
            UPDATE game_tiles 
            SET location = 'POOL', table_set_id = NULL, participant_id = NULL 
            WHERE game_id = $1 AND (location = 'TABLE' OR participant_id = $2)
        `, [gameId, participantId]);

        // 2. Insert newTableSets dan update game_tiles
        for (const set of newTableSets) {
            const setRes = await client.query(`
                INSERT INTO table_sets (game_id, set_type) VALUES ($1, $2) RETURNING id
            `, [gameId, set.set_type]);
            const newSetId = setRes.rows[0].id;

            // Update ubin ini menjadi TABLE dan masuk ke set ini
            for (const tId of set.tile_ids) {
                await client.query(`
                    UPDATE game_tiles
                    SET location = 'TABLE', table_set_id = $1, participant_id = NULL
                    WHERE game_id = $2 AND tile_id = $3
                `, [newSetId, gameId, tId]);
            }
        }

        // 3. Update sisa ubin ke RACK participant
        if (newRackTileIds && newRackTileIds.length > 0) {
            const placeholders = newRackTileIds.map((_, i) => `$${i + 3}`).join(', ');
            await client.query(`
                UPDATE game_tiles
                SET location = 'RACK', participant_id = $1, table_set_id = NULL
                WHERE game_id = $2 AND tile_id IN (${placeholders})
            `, [participantId, gameId, ...newRackTileIds]);
        }

        await client.query('COMMIT');
        return true;
    } catch (error) {
        await client.query('ROLLBACK');
        throw error;
    } finally {
        client.release();
    }
};

const getParticipantRackTileCount = async (gameId, participantId) => {
    const query = `SELECT COUNT(*) AS count FROM game_tiles WHERE game_id = $1 AND participant_id = $2 AND location = 'RACK'`;
    const res = await db.query(query, [gameId, participantId]);
    return parseInt(res.rows[0].count);
};

module.exports = {
    getAllMasterTiles,
    setupGameTiles,
    drawTileFromPool,
    getBoardAndRackState,
    applyTurnStateTransaction,
    getParticipantRackTileCount
};
