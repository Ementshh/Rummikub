const express = require('express');
const router = express.Router();
const gameController = require('../controllers/gameController');
const { verifyToken } = require('../middlewares/authMiddleware');

router.use(verifyToken); // Semua endpoint game dilindungi JWT

router.post('/create', gameController.createGame);
router.post('/:id/join', gameController.joinGame);
router.post('/:id/start', gameController.startGame);
router.post('/:id/end-turn', gameController.endTurn);
router.post('/:id/draw', gameController.drawTile);

module.exports = router;
