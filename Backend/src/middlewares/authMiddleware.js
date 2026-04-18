const jwt = require('jsonwebtoken');

const verifyToken = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'Akses ditolak. Token tidak ditemukan.' });
    }

    const token = authHeader.split(' ')[1];

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.user = decoded; // { userId: uuid, username: string }
        next();
    } catch (error) {
        return res.status(401).json({ error: 'Token tidak valid atau sudah kedaluwarsa.' });
    }
};

module.exports = {
    verifyToken
};
