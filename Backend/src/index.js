require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');

const authRoutes = require('./routes/authRoutes');
const gameRoutes = require('./routes/gameRoutes');
const errorHandler = require('./middlewares/errorHandler');
const rumLogic = require('./services/rummikubLogicService');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(helmet());
app.use(cors());
app.use(express.json());

app.use('/api/auth', authRoutes);
app.use('/api/games', gameRoutes);

app.use(errorHandler);

// Inisialisasi tile properties ke memory sebelum start server
rumLogic.initTiles().then(() => {
    console.log('✅ Tile dictionary loaded to memory.');
    app.listen(PORT, () => {
        console.log(`🚀 Rummikub Backend running on port ${PORT}`);
    });
}).catch(err => {
    console.error('Failed to load tile dict:', err);
});
