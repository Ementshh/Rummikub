const gameRepository = require('../repositories/gameRepository');
const tileRepository = require('../repositories/tileRepository');
const turnValidator = require('./turnValidatorService');

const createGame = async () => {
    return await gameRepository.createGame();
};

const joinGame = async (gameId, userId) => {
    const game = await gameRepository.getGameById(gameId);
    if (!game) throw { statusCode: 404, message: 'Game tidak ditemukan.' };
    if (game.status !== 'WAITING') throw { statusCode: 400, message: 'Game sudah dimulai atau selesai.' };

    const participants = await gameRepository.getParticipants(gameId);
    if (participants.length >= 4) throw { statusCode: 400, message: 'Ruangan sudah penuh (Maks 4 pemain).' };

    const alreadyJoined = participants.find(p => p.user_id === userId);
    if (alreadyJoined) throw { statusCode: 400, message: 'Anda sudah bergabung dalam permainan ini.' };

    const turnOrder = participants.length + 1;
    return await gameRepository.joinGame(gameId, userId, turnOrder);
};

const startGame = async (gameId) => {
    const game = await gameRepository.getGameById(gameId);
    if (game.status !== 'WAITING') throw { statusCode: 400, message: 'Game sudah berjalan.' };

    const participants = await gameRepository.getParticipants(gameId);
    if (participants.length < 2) throw { statusCode: 400, message: 'Minimal 2 pemain untuk memulai.' };

    // 1. Set status menjadi IN_PROGRESS dan tetapkan giliran pertama
    const currentTurnParticipant = participants.find(p => p.turn_order === 1);
    await gameRepository.updateGameStatus(gameId, 'IN_PROGRESS', currentTurnParticipant.id);

    // 2. Setup 106 Ubin ke dalam Pool
    await tileRepository.setupGameTiles(gameId);

    // 3. Bagi 14 Ubin secara acak ke setiap pemain dari POOL
    for (const p of participants) {
        for (let i = 0; i < 14; i++) {
            await tileRepository.drawTileFromPool(gameId, p.id);
        }
    }

    return { message: 'Game berhasil dimulai!', firstTurnId: currentTurnParticipant.id };
};

const defineNextTurn = async (gameId, currentParticipantId) => {
    const participants = await gameRepository.getParticipants(gameId);
    const currIdx = participants.findIndex(p => p.id === currentParticipantId);
    let nextIdx = currIdx + 1;
    if (nextIdx >= participants.length) nextIdx = 0;

    const nextParticipantId = participants[nextIdx].id;
    await gameRepository.switchTurn(gameId, nextParticipantId);
    return nextParticipantId;
};

const executeEndTurn = async (gameId, userId, newTableSets, newRackTileIds) => {
    // Basic validations
    const participant = await gameRepository.getParticipantByUserAndGame(gameId, userId);
    if (!participant) throw { statusCode: 403, message: 'Anda bukan peserta game ini.' };

    const game = await gameRepository.getGameById(gameId);
    if (game.status !== 'IN_PROGRESS') throw { statusCode: 400, message: 'Game tidak sedang berlangsung.' };
    if (game.current_turn_participant_id !== participant.id) throw { statusCode: 403, message: 'Bukan giliran Anda.' };

    turnValidator.participantCache = participant; // Dirty trick or just inject
    
    // Check timeout manually if passing 2 minutes limit
    const turnDurationSeconds = (new Date() - new Date(game.turn_started_at)) / 1000;
    if (turnDurationSeconds > 125) { // Toleransi 5 detik untuk latency
        // Jika melebihi waktu yg ditentukan, tolak perpindahan ubin.
        throw { statusCode: 408, message: 'Waktu giliran Anda sudah habis. Silakan endpoint draw/penalty!' };
    }

    // Wrap the old call and inject participant
    const _turnValidator = require('./turnValidatorService'); 
    
    const result = await _turnValidator.executeEndTurn(gameId, participant.id, newTableSets, newRackTileIds);
    if (result.isFinished) return { message: 'Game Selesai! Anda menang.', winner: participant.id };

    const nextPId = await defineNextTurn(gameId, participant.id);
    return { message: 'Giliran berhasil diselesaikan.', nextTurnId: nextPId };
};

const drawTilePenalty = async (gameId, userId) => {
    const participant = await gameRepository.getParticipantByUserAndGame(gameId, userId);
    const game = await gameRepository.getGameById(gameId);

    if (game.current_turn_participant_id !== participant.id) throw { statusCode: 403, message: 'Bukan giliran Anda.' };

    // Ubin hanya diambil jika tidak membongkar meja sama sekali. 
    // Tarik 1 kartu ke rak pemain.
    const drawnTileId = await tileRepository.drawTileFromPool(gameId, participant.id);
    
    if (!drawnTileId) {
        throw { statusCode: 400, message: 'Pool sudah habis, tapi giliran diputar.' };
    }

    const nextPId = await defineNextTurn(gameId, participant.id);
    return { message: 'Ubin diambil. Giliran berpindah.', nextTurnId: nextPId, drawnTileId };
};

module.exports = {
    createGame,
    joinGame,
    startGame,
    executeEndTurn,
    drawTilePenalty
};
