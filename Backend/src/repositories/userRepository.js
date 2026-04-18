const db = require('../config/database');

const createUser = async (username, passwordHash) => {
    const query = `
        INSERT INTO users (username, password_hash)
        VALUES ($1, $2)
        RETURNING id, username, created_at;
    `;
    const values = [username, passwordHash];
    const result = await db.query(query, values);
    return result.rows[0];
};

const getUserByUsername = async (username) => {
    const query = `
        SELECT * FROM users WHERE username = $1;
    `;
    const result = await db.query(query, [username]);
    return result.rows[0];
};

const getUserById = async (id) => {
    const query = `
        SELECT id, username, created_at FROM users WHERE id = $1;
    `;
    const result = await db.query(query, [id]);
    return result.rows[0];
};

module.exports = {
    createUser,
    getUserByUsername,
    getUserById
};
