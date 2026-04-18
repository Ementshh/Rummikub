const gameService = require('../services/gameService');

const createGame = async (req, res, next) => {
    try {
        const game = await gameService.createGame();
        res.status(201).json({ success: true, data: game });
    } catch (error) {
        next(error);
    }
};

const joinGame = async (req, res, next) => {
    try {
        const gameId = req.params.id;
        const userId = req.user.userId;
        const participant = await gameService.joinGame(gameId, userId);
        res.status(200).json({ success: true, data: participant });
    } catch (error) {
        next(error);
    }
};

const startGame = async (req, res, next) => {
    try {
        const gameId = req.params.id;
        const result = await gameService.startGame(gameId);
        res.status(200).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

const endTurn = async (req, res, next) => {
    try {
        const gameId = req.params.id;
        const userId = req.user.userId;
        const { table_sets, rack_tiles } = req.body;
        
        if (!Array.isArray(table_sets) || !Array.isArray(rack_tiles)) {
            return res.status(400).json({ success: false, error: 'Format body salah. table_sets dan rack_tiles harus berupa array.' });
        }

        const result = await gameService.executeEndTurn(gameId, userId, table_sets, rack_tiles);
        res.status(200).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

const drawTile = async (req, res, next) => {
    try {
        const gameId = req.params.id;
        const userId = req.user.userId;
        const result = await gameService.drawTilePenalty(gameId, userId);
        res.status(200).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    createGame,
    joinGame,
    startGame,
    endTurn,
    drawTile
};
