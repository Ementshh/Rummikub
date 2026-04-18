const authService = require('../services/authService');

const register = async (req, res, next) => {
    try {
        const { username, password } = req.body;
        if (!username || !password) {
            return res.status(400).json({ success: false, error: 'Username dan password wajib diisi.' });
        }
        
        const user = await authService.register(username, password);
        res.status(201).json({ success: true, data: user });
    } catch (error) {
        next(error);
    }
};

const login = async (req, res, next) => {
    try {
        const { username, password } = req.body;
        if (!username || !password) {
            return res.status(400).json({ success: false, error: 'Username dan password wajib diisi.' });
        }
        
        const result = await authService.login(username, password);
        res.status(200).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    register,
    login
};
