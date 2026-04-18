const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const userRepository = require('../repositories/userRepository');

const register = async (username, password) => {
    const existingUser = await userRepository.getUserByUsername(username);
    if (existingUser) {
        const error = new Error('Username sudah terdaftar.');
        error.statusCode = 400;
        throw error;
    }

    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    const newUser = await userRepository.createUser(username, passwordHash);
    return newUser;
};

const login = async (username, password) => {
    const user = await userRepository.getUserByUsername(username);
    if (!user) {
        const error = new Error('Username atau password salah.');
        error.statusCode = 401;
        throw error;
    }

    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
        const error = new Error('Username atau password salah.');
        error.statusCode = 401;
        throw error;
    }

    const token = jwt.sign(
        { userId: user.id, username: user.username },
        process.env.JWT_SECRET,
        { expiresIn: '24h' }
    );

    return { token, user: { id: user.id, username: user.username } };
};

module.exports = {
    register,
    login
};
