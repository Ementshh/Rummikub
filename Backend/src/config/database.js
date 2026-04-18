const { Pool } = require('pg');
require('dotenv').config();

const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: {
        rejectUnauthorized: false
    }
});

pool.on('error', (err, client) => {
    console.error('Unexpected error on idle client', err);
    process.exit(-1);
});

module.exports = {
    // Digunakan untuk query biasa tanpa transaction
    query: (text, params) => pool.query(text, params),
    // Mendapatkan client khusus untuk manual transaction
    getClient: () => pool.connect(),
};
