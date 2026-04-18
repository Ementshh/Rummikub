const db = require('../config/database');

const createGame = async () => {
    const query = `
        INSERT INTO games (status)
        VALUES ('WAITING')
        RETURNING *;
    `;
    const result = await db.query(query);
    return result.rows[0];
};

const getGameById = async (gameId) => {
    const query = `SELECT * FROM games WHERE id = $1;`;
    const result = await db.query(query, [gameId]);
    return result.rows[0];
};

const getParticipants = async (gameId) => {
    const query = `
        SELECT gp.*, u.username 
        FROM game_participants gp
        JOIN users u ON gp.user_id = u.id
        WHERE gp.game_id = $1
        ORDER BY gp.turn_order ASC;
    `;
    const result = await db.query(query, [gameId]);
    return result.rows;
};

const getParticipantByUserAndGame = async (gameId, userId) => {
    const query = `SELECT * FROM game_participants WHERE game_id = $1 AND user_id = $2;`;
    const result = await db.query(query, [gameId, userId]);
    return result.rows[0];
};

const joinGame = async (gameId, userId, turnOrder) => {
    const query = `
        INSERT INTO game_participants (game_id, user_id, turn_order)
        VALUES ($1, $2, $3)
        RETURNING *;
    `;
    const result = await db.query(query, [gameId, userId, turnOrder]);
    return result.rows[0];
};

const updateGameStatus = async (gameId, status, currentTurnParticipantId = null) => {
    const query = `
        UPDATE games 
        SET status = $1, 
            current_turn_participant_id = COALESCE($2, current_turn_participant_id),
            turn_started_at = CURRENT_TIMESTAMP
        WHERE id = $3
        RETURNING *;
    `;
    const result = await db.query(query, [status, currentTurnParticipantId, gameId]);
    return result.rows[0];
};

const updateParticipantInitialMeld = async (participantId) => {
    const query = `UPDATE game_participants SET has_done_initial_meld = TRUE WHERE id = $1 RETURNING *;`;
    const result = await db.query(query, [participantId]);
    return result.rows[0];
};

const switchTurn = async (gameId, nextParticipantId) => {
    const query = `
        UPDATE games 
        SET current_turn_participant_id = $1, turn_started_at = CURRENT_TIMESTAMP
        WHERE id = $2 RETURNING *;
    `;
    const result = await db.query(query, [nextParticipantId, gameId]);
    return result.rows[0];
};

module.exports = {
    createGame,
    getGameById,
    getParticipants,
    getParticipantByUserAndGame,
    joinGame,
    updateGameStatus,
    updateParticipantInitialMeld,
    switchTurn
};
