const tileRepository = require('../repositories/tileRepository');
const gameRepository = require('../repositories/gameRepository');
const rumLogic = require('./rummikubLogicService');

const executeEndTurn = async (gameId, participantId, newTableSets, newRackTileIds) => {
    // 1. Ambil state lama
    const oldState = await tileRepository.getBoardAndRackState(gameId, participantId);
    
    // Simpan list ID untuk integritas
    const oldTableTiles = oldState.filter(gt => gt.location === 'TABLE').map(gt => gt.tile_id);
    const oldRackTiles = oldState.filter(gt => gt.location === 'RACK').map(gt => gt.tile_id);
    const allOldTiles = [...oldTableTiles, ...oldRackTiles];

    // Simpan list ID yang diusulkan
    let allNewTableTiles = [];
    for (const set of newTableSets) {
        allNewTableTiles.push(...set.tile_ids);
    }
    const allNewTiles = [...allNewTableTiles, ...newRackTileIds];

    // 2. CEK INTEGRITAS (Ubin tidak boleh hilang atau muncul tiba-tiba dari pool/musuh)
    const oldSorted = [...allOldTiles].sort((a,b)=>a-b);
    const newSorted = [...allNewTiles].sort((a,b)=>a-b);
    
    if (oldSorted.length !== newSorted.length || !oldSorted.every((val, index) => val === newSorted[index])) {
        throw { statusCode: 400, message: 'Integritas ubin gagal: Jumlah ubin tidak sesuai dengan yang dimiliki di awal giliran.' };
    }

    // Identifikasi Ubin mana aja yang berpindah dari Rack ke Table
    const tableNewSet = new Set(allNewTableTiles);
    const tableOldSet = new Set(oldTableTiles);
    const playerMovedTiles = [];
    
    for (const tId of allNewTableTiles) {
        if (!tableOldSet.has(tId)) {
            playerMovedTiles.push(tId);
        }
    }

    if (playerMovedTiles.length === 0) {
        throw { statusCode: 400, message: 'Harus ada ubin yang diturunkan, atau tarik dari pool (draw).' };
    }

    // 3. Validasi Tiap Board Set (Semua set di meja HARUS valid)
    for (const set of newTableSets) {
        const valRes = await rumLogic.isValidSet(set.set_type, set.tile_ids);
        if (!valRes.valid) {
            throw { statusCode: 400, message: valRes.reason };
        }
    }

    // 4. Syarat Initial Meld (Jika pemain belum open)
    const participant = await gameRepository.getParticipantByUserAndGame(gameId, null); // Actually we need participant by ID
    // Lets fetch via repository. But wait, I'll fetch participants in gameService and pass participant obj.
    // Let's assume participant passed is the full row object
    if (!participant.has_done_initial_meld) {
        const meldCheck = await rumLogic.checkInitialMeld(newTableSets, playerMovedTiles, oldTableTiles);
        if (!meldCheck.valid) {
            throw { statusCode: 400, message: meldCheck.reason };
        }
        // Jika lolos, update status meldnya!
        await gameRepository.updateParticipantInitialMeld(participantId);
    }

    // 5. Simpan Perubahan ke Database (Transaksi)
    await tileRepository.applyTurnStateTransaction(gameId, participantId, newTableSets, newRackTileIds);

    // 6. Cek Winner
    if (newRackTileIds.length === 0) {
        await gameRepository.updateGameStatus(gameId, 'FINISHED', null);
        return { isFinished: true, winnerId: participantId };
    }

    return { isFinished: false };
};

module.exports = {
    executeEndTurn
};
