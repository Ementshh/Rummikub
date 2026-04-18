const tileRepository = require('../repositories/tileRepository');

let tileDict = null;

const initTiles = async () => {
    if (!tileDict) {
        const tiles = await tileRepository.getAllMasterTiles();
        tileDict = {};
        for (const t of tiles) {
            tileDict[t.id] = t;
        }
    }
    return tileDict;
};

// Fungsi mengecek validitas sebuah SET (GROUP atau RUN)
const isValidSet = async (setType, tileIds) => {
    const dict = await initTiles();

    if (!tileIds || tileIds.length < 3) return { valid: false, reason: 'Set minimum 3 ubin.' };
    if (tileIds.length > 13) return { valid: false, reason: 'Set melebihi batas 13.' };

    const tiles = tileIds.map(id => dict[id]);
    const nonJokers = tiles.filter(t => !t.is_joker);

    if (nonJokers.length === 0) return { valid: true, value: 30 }; // Semua joker (rare but possible 2 joker max in standard)

    if (setType === 'GROUP') {
        if (tileIds.length > 4) return { valid: false, reason: 'Group maksimal 4 ubin.' };

        const targetNumber = nonJokers[0].number;
        const colorsSeen = new Set();
        let valueSum = 0;

        for (const t of tiles) {
            if (t.is_joker) {
                valueSum += targetNumber; // Joker mengambil nilai angka grup
                continue;
            }
            if (t.number !== targetNumber) return { valid: false, reason: 'Angka dalam Group berbeda.' };
            if (colorsSeen.has(t.color)) return { valid: false, reason: 'Warna sama tidak valid dalam Group.' };
            colorsSeen.add(t.color);
            valueSum += t.number;
        }
        return { valid: true, value: valueSum };

    } else if (setType === 'RUN') {
        // Kita asumsikan tileIds datang berurutan dari ujung kiri ke kanan
        const targetColor = nonJokers[0].color;
        let valueSum = 0;

        // Cari tahu nilai angka pertama di run untuk mengecek urutan
        let currentNumber = -1;
        for (let i = 0; i < tiles.length; i++) {
            if (!tiles[i].is_joker) {
                currentNumber = tiles[i].number - i; // Dapatkan titik awal konseptual
                break;
            }
        }

        if (currentNumber < 1) return { valid: false, reason: 'Run out of bounds di bawah 1.' };

        for (let i = 0; i < tiles.length; i++) {
            const t = tiles[i];
            const expectedNumber = currentNumber + i;
            if (expectedNumber > 13) return { valid: false, reason: 'Run melebihi angka 13.' };

            if (!t.is_joker) {
                if (t.color !== targetColor) return { valid: false, reason: 'Beda warna dalam Run.' };
                if (t.number !== expectedNumber) return { valid: false, reason: 'Angka tidak berurutan.' };
            }
            valueSum += expectedNumber; // Joker mendapat nilai angka yang digantikan
        }
        return { valid: true, value: valueSum };
    }

    return { valid: false, reason: 'Tipe set tidak dikenal.' };
};

// Mengecek syarat Initial Meld (>= 30 poin, murni dari rak, tidak membongkar meja)
// playerMovedTiles adalah list ID ubin yang diambil dari rak pemain di giliran ini.
// newTableSets adalah usulan board baru. 
// oldBoardTiles adalah list ID ubin yang dulunya ada di meja.
const checkInitialMeld = async (newTableSets, playerMovedTiles, oldBoardTiles) => {
    // Pada saat Initial Meld, pemain TIDAK BOLEH memanipulasi ubin meja lama sama sekali.
    // Artinya, semua ubin meja lama harus tetap utuh bentuk setnya. Karena ini cukup sulit dilacak jika digabung, 
    // simpelnya: kita harus mencari di dalam `newTableSets` set manakah yang SPESIFIK 100% dibuat MURNI 
    // dari `playerMovedTiles`. Dan kita total valuenya.
    // Jika ada ubin dari playerMovedTiles yang NANGKRING di set yg juga berisi oldBoardTiles (melakukan melding/nyambung),
    // itu dilarang keras saat initial meld.

    const dict = await initTiles();
    let meldPoints = 0;

    const oldBoardSet = new Set(oldBoardTiles);
    const playerMovedSet = new Set(playerMovedTiles);

    for (const set of newTableSets) {
        let hasOld = false;
        let hasNew = false;
        for (const tId of set.tile_ids) {
            if (oldBoardSet.has(tId)) hasOld = true;
            if (playerMovedSet.has(tId)) hasNew = true;
        }

        if (hasOld && hasNew) {
            return { valid: false, reason: 'Initial Meld tidak boleh memodifikasi atau menempel pada set yang sudah ada di meja.' };
        }

        if (hasNew && !hasOld) { // Ini set murni buatan dari pemain
            const validationResult = await isValidSet(set.set_type, set.tile_ids);
            if (!validationResult.valid) {
                return { valid: false, reason: `Set baru dari rak tidak valid: ${validationResult.reason}` };
            }
            meldPoints += validationResult.value;
        }
    }

    if (meldPoints < 30) {
        return { valid: false, reason: `Poin Initial Meld kurang dari 30 (Hanya ${meldPoints}).` };
    }

    return { valid: true };
};

module.exports = {
    initTiles,
    isValidSet,
    checkInitialMeld
};
