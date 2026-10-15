const inputSpectatorRoom = document.getElementById('spectator-room-number');
const btnSpectatorRefresh = document.getElementById('btn-spectator-refresh');
const spectatorStatus = document.getElementById('spectator-status');
const spectatorGoodList = document.getElementById('spectator-good-list');
const spectatorEvilList = document.getElementById('spectator-evil-list');

let currentRoomNumber = null;

function updateStatus(text) {
    if (spectatorStatus) {
        spectatorStatus.textContent = text;
    }
}

function renderPlayers(players) {
    if (!spectatorGoodList || !spectatorEvilList) return;
    
    if (!players || players.length === 0) {
        spectatorGoodList.innerHTML = '<div style="text-align:center;padding:10px;color:rgba(255,255,255,0.5);">暂无数据</div>';
        spectatorEvilList.innerHTML = '<div style="text-align:center;padding:10px;color:rgba(255,255,255,0.5);">暂无数据</div>';
        return;
    }
    
    // 分离好人和坏人，并按座位号排序
    const goodPlayers = players
        .filter(p => p.camp === 'good')
        .sort((a, b) => (a.seatNumber || 0) - (b.seatNumber || 0));
    
    const evilPlayers = players
        .filter(p => p.camp === 'evil')
        .sort((a, b) => (a.seatNumber || 0) - (b.seatNumber || 0));
    
    // 渲染好人列表
    if (goodPlayers.length === 0) {
        spectatorGoodList.innerHTML = '<div style="text-align:center;padding:10px;color:rgba(255,255,255,0.5);">暂无</div>';
    } else {
        spectatorGoodList.innerHTML = goodPlayers.map(player => {
            return `<div class="spectator-item good">
                <span class="spectator-seat">${player.seatNumber || '-'}</span>
                <span class="spectator-identity">${player.identity || '未知'}</span>
            </div>`;
        }).join('');
    }
    
    // 渲染坏人列表
    if (evilPlayers.length === 0) {
        spectatorEvilList.innerHTML = '<div style="text-align:center;padding:10px;color:rgba(255,255,255,0.5);">暂无</div>';
    } else {
        spectatorEvilList.innerHTML = evilPlayers.map(player => {
            return `<div class="spectator-item evil">
                <span class="spectator-seat">${player.seatNumber || '-'}</span>
                <span class="spectator-identity">${player.identity || '未知'}</span>
            </div>`;
        }).join('');
    }
}

const apiPrefix = (() => {
    const path = window.location.pathname || '';
    const segments = path.split('/').filter(Boolean);
    if (segments.length > 0) {
        return `/${segments[0]}/api`;
    }
    return '/api';
})();

function getSpectatorParam(name) {
    const searchParams = new URLSearchParams(window.location.search || '');
    if (searchParams.has(name)) {
        return searchParams.get(name);
    }
    const hash = window.location.hash || '';
    if (hash) {
        const queryIndex = hash.indexOf('?');
        let hashQuery = queryIndex >= 0 ? hash.substring(queryIndex + 1) : hash.substring(1);
        if (hashQuery) {
            const hashParams = new URLSearchParams(hashQuery);
            if (hashParams.has(name)) {
                return hashParams.get(name);
            }
        }
    }
    return null;
}

async function fetchSpectatorRoom(roomNumber) {
    if (!roomNumber) {
        updateStatus('请先输入房间号');
        return;
    }
    updateStatus(`正在加载房间 ${roomNumber} ...`);
    try {
        const response = await fetch(`${apiPrefix}/rooms/${roomNumber}/spectator`);
        if (!response.ok) {
            throw new Error('房间不存在或已过期');
        }
        const data = await response.json();
        currentRoomNumber = roomNumber;
        updateStatus(`房间 ${roomNumber} | 总人数 ${data.totalPlayers} | 游戏状态：${data.gameStarted ? '已开始' : '未开始'} | 当前领袖：${data.currentLeaderSeat || '-'} 号`);
        renderPlayers(data.players || []);
    } catch (e) {
        updateStatus(e.message || '加载失败');
        renderPlayers([]);
    }
}

btnSpectatorRefresh?.addEventListener('click', () => {
    fetchSpectatorRoom(inputSpectatorRoom.value.trim());
});

inputSpectatorRoom?.addEventListener('keyup', (event) => {
    if (event.key === 'Enter') {
        fetchSpectatorRoom(inputSpectatorRoom.value.trim());
    }
});

window.addEventListener('load', () => {
    const roomParam = getSpectatorParam('room');
    if (roomParam) {
        inputSpectatorRoom.value = roomParam;
        fetchSpectatorRoom(roomParam);
    }
});

