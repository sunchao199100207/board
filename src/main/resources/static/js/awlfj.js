function onPlayerInfo(payload) {
    if (!payload || !payload.body) return;
    try {
        const data = JSON.parse(payload.body);
        if (data && data.playerId) {
            myPlayerId = data.playerId;
            localStorage.setItem('avalonPlayerId', myPlayerId);
            console.log("🎯 [玩家信息] 收到 playerId: " + myPlayerId);
            schedulePreferredRoleUpdate();
        }
    } catch (e) {
        console.error("解析 player-info 消息失败:", e);
    }
}
let stompClient = null;
let currentRoom = null;
let mySessionId = null;
let myPlayerId = localStorage.getItem('avalonPlayerId');
let mySelectedSeat = 0;
let roomSubscription = null;
// 保存所有订阅引用，以便在断开连接时清理
let messageSubscriptions = {
    errors: null,
    roomCreated: null,
    gameStart: null,
    playerInfo: null,
    roomState: null
};
// 创建房间超时定时器
let createRoomTimeout = null;

// 调试控制台相关变量
let debugConsoleLogs = [];
let debugConsoleMaxLogs = 500;
let originalConsole = {
    log: console.log,
    warn: console.warn,
    error: console.error,
    info: console.info
};

const pages = {
    home: document.getElementById('page-home'),
    room: document.getElementById('page-room'),
    identity: document.getElementById('page-identity'),
    mission: document.getElementById('page-mission'),
    voting: document.getElementById('page-voting'),
    missionResult: document.getElementById('page-mission-result')
};
const homeError = document.getElementById('home-error');
const roomError = document.getElementById('room-error');
const inputRoomNumber = document.getElementById('room-number');
const sliderTotalPlayers = document.getElementById('total-players');
const sliderValueDisplay = document.getElementById('total-players-value');
const btnCreateRoom = document.getElementById('btn-create-room');
const btnJoinRoom = document.getElementById('btn-join-room');
const roomInfoNumber = document.getElementById('room-info-number');
const seatGrid = document.getElementById('seat-selection-grid');
const roomStatusText = document.getElementById('room-status-text');
const btnLeaveRoom = document.getElementById('btn-leave-room');
const btnReady = document.getElementById('btn-ready');
const identityImage = document.getElementById('identity-image');
const identityName = document.getElementById('identity-name');
const identityCard = document.getElementById('identity-card');
const seenTitle = document.getElementById('seen-title');
const seenPlayers = document.getElementById('seen-players');
const seenSubtitle = document.getElementById('seen-subtitle');
const leaderSeatNumber = document.getElementById('leader-seat-number');
const btnReturnToRoom = document.getElementById('btn-return-to-room');
const btnStartMission = document.getElementById('btn-start-mission');
const loaderOverlay = document.getElementById('loader-overlay');
const loaderText = document.getElementById('loader-text');

// 任务相关元素
const missionSeatGrid = document.getElementById('mission-seat-grid');
const missionRoundNumber = document.getElementById('mission-round-number');
const missionLeaderSeat = document.getElementById('mission-leader-seat');
const missionRequiredSize = document.getElementById('mission-required-size');
const missionError = document.getElementById('mission-error');
const btnConfirmMission = document.getElementById('btn-confirm-mission');
const btnCancelMission = document.getElementById('btn-cancel-mission');

// 投票相关元素
const votingRoundNumber = document.getElementById('voting-round-number');
const votingMembers = document.getElementById('voting-members');
const votingError = document.getElementById('voting-error');
const btnVoteSuccess = document.getElementById('btn-vote-success');
const btnVoteFail = document.getElementById('btn-vote-fail');
const votingStatus = document.getElementById('voting-status');

// 任务结果相关元素
const resultRoundNumber = document.getElementById('result-round-number');
const missionResultDisplay = document.getElementById('mission-result-display');
const btnBackToIdentity = document.getElementById('btn-back-to-identity');
const btnBackToIdentityFromVoting = document.getElementById('btn-back-to-identity-from-voting');
const missionHistoryContent = document.getElementById('mission-history-content');
// btnReconnect 在 updateReconnectButton 中动态获取，因为可能在 DOMContentLoaded 之前不存在

// 湖中仙女相关元素
const lakeLadySection = document.getElementById('lake-lady-section');
const lakeLadyInspectSection = document.getElementById('lake-lady-inspect-section');
const lakeLadyResultSection = document.getElementById('lake-lady-result-section');
const lakeLadySeatGrid = document.getElementById('lake-lady-seat-grid');
const lakeLadyInspectResult = document.getElementById('lake-lady-inspect-result');
const lakeLadySubtitle = document.getElementById('lake-lady-subtitle');

let selectedMissionSeats = new Set();

// 防止重复连接的标志
let isConnecting = false;
// 防止重复发送加入房间请求的标志
let isJoiningRoom = false;
let lastJoinRoomAttempt = null;

function getUrlParamValue(name) {
    const searchParams = new URLSearchParams(window.location.search || '');
    if (searchParams.has(name)) {
        return searchParams.get(name);
    }
    const hash = window.location.hash || '';
    if (hash) {
        const queryIndex = hash.indexOf('?');
        let hashQuery = queryIndex >= 0 ? hash.substring(queryIndex + 1) : hash.substring(1);
        const anchorIndex = hashQuery.indexOf('?');
        if (anchorIndex >= 0) {
            hashQuery = hashQuery.substring(anchorIndex + 1);
        }
        if (hashQuery) {
            const hashParams = new URLSearchParams(hashQuery);
            if (hashParams.has(name)) {
                return hashParams.get(name);
            }
        }
    }
    return null;
}

const isDebugConsoleEnabled = (getUrlParamValue('test') || '').toLowerCase() === 'true';
const preferredRoleMap = {
    '1': '梅林',
    '2': '派西维尔',
    '3': '亚瑟的忠臣'
};
let preferredRoleOverride = null;
let preferredRoleClearedFlag = false;
const PREFERRED_ROLE_CLEAR_TOKEN = '__CLEAR__';
let pendingPreferredRoleSync = false;
let lastReadyState = false;

(function initPreferredRole() {
    syncPreferredRoleFromUrl('init');
})();

window.addEventListener('hashchange', () => syncPreferredRoleFromUrl('hashchange'));
window.addEventListener('popstate', () => syncPreferredRoleFromUrl('popstate'));
window.addEventListener('pageshow', () => syncPreferredRoleFromUrl('pageshow'));

function syncPreferredRoleFromUrl(reason = 'update') {
    const roleParam = getUrlParamValue('role');
    if (roleParam && preferredRoleMap[roleParam]) {
        preferredRoleOverride = preferredRoleMap[roleParam];
        localStorage.setItem('avalonPreferredRole', preferredRoleOverride);
        preferredRoleClearedFlag = false;
        console.log(`🔄 [角色偏好] (${reason}) 已设置首选身份：${preferredRoleOverride}`);
    } else {
        const storedRole = localStorage.getItem('avalonPreferredRole');
        if (storedRole) {
            localStorage.removeItem('avalonPreferredRole');
            preferredRoleOverride = null;
            preferredRoleClearedFlag = true;
            console.log(`🔄 [角色偏好] (${reason}) 角色参数已移除，清空首选身份`);
        } else {
            preferredRoleOverride = null;
            preferredRoleClearedFlag = false;
            console.log(`🔄 [角色偏好] (${reason}) 无角色参数，保持未设置状态`);
        }
    }
    schedulePreferredRoleUpdate();
}

function getPreferredRolePayloadOptions() {
    if (preferredRoleOverride && preferredRoleOverride.trim().length > 0) {
        return { value: preferredRoleOverride, cleared: false };
    }
    if (preferredRoleClearedFlag) {
        return { value: null, cleared: true };
    }
    return { value: null, cleared: false };
}

function buildJoinPayload(roomNumber) {
    const payload = { roomNumber };
    attachPreferredRole(payload, 'join-room');
    return payload;
}

function buildRejoinPayload(roomNumber, playerId) {
    const payload = { roomNumber, playerId };
    attachPreferredRole(payload, 'rejoin-with-id');
    return payload;
}

function buildCreatePayload(roomNumber, totalPlayers) {
    const payload = { roomNumber, totalPlayers };
    attachPreferredRole(payload, 'create-room');
    return payload;
}

function attachPreferredRole(payload, context) {
    if (!payload) return;
    const { value, cleared } = getPreferredRolePayloadOptions();
    if (value) {
        payload.preferredRole = value;
        preferredRoleClearedFlag = false;
        console.log(`🎭 [角色偏好] ${context} payload：${value}`);
    } else if (cleared) {
        payload.preferredRole = PREFERRED_ROLE_CLEAR_TOKEN;
        preferredRoleClearedFlag = false;
        console.log(`🎭 [角色偏好] ${context} payload：清除首选身份`);
    } else {
        console.log(`🎭 [角色偏好] ${context} payload：未设置`);
    }
}

function schedulePreferredRoleUpdate() {
    if (pendingPreferredRoleSync) return;
    pendingPreferredRoleSync = true;
    setTimeout(() => {
        pendingPreferredRoleSync = false;
        sendPreferredRoleUpdate();
    }, 0);
}

function sendPreferredRoleUpdate() {
    if (!stompClient || !stompClient.connected || !currentRoom || !currentRoom.roomNumber || !myPlayerId) {
        return;
    }
    const payload = { roomNumber: currentRoom.roomNumber, playerId: myPlayerId };
    attachPreferredRole(payload, 'update-preferred-role');
    stompClient.send("/app/update-preferred-role", {}, JSON.stringify(payload));
}
function connect(onConnectedCallback, isReconnect = false) {
    // 如果正在连接中，忽略重复请求
    if (isConnecting) {
        console.log("🔄 [连接] 正在连接中，忽略重复请求");
        return;
    }
    
    if (stompClient && stompClient.connected) {
        console.log("🔄 [连接] 已连接，先断开再重连，当前 sessionId=" + mySessionId);
        // 清空旧的 sessionId，确保重新连接时使用新的 sessionId
        const oldSessionId = mySessionId;
        mySessionId = null;
        disconnectCleanup();
        if (stompClient) {
            stompClient.disconnect(() => {
                console.log("🔄 [连接] 旧连接已断开，旧 sessionId=" + oldSessionId);
                performConnect(onConnectedCallback, isReconnect);
            }, {});
        } else {
            performConnect(onConnectedCallback, isReconnect);
        }
    } else {
        // 如果未连接，也清空旧的 sessionId
        mySessionId = null;
        performConnect(onConnectedCallback, isReconnect);
    }
}

function performConnect(onConnectedCallback, isReconnect = false) {
    // 设置连接标志
    isConnecting = true;
    
    // 重连时不清空 currentRoom，保留房间信息以便后续恢复
    if (!isReconnect) {
        disconnectCleanup();
    } else {
        // 重连时只清理订阅，不清空房间信息
        if (roomSubscription) {
            try {
                roomSubscription.unsubscribe();
            } catch (e) {}
            roomSubscription = null;
        }
    }
    
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        isConnecting = false;
        showError('home-error', '页面资源加载不全');
        return;
    }

    try {
        const socket = new SockJS('ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = () => {};

        stompClient.connect({}, (frame) => {
            // 连接成功，清除连接标志
            isConnecting = false;
            try {
                // 尝试多种方式获取 Session ID
                let transportUrl = null;
                if (stompClient.ws && stompClient.ws._transport && stompClient.ws._transport.url) {
                    transportUrl = stompClient.ws._transport.url;
                } else if (stompClient.ws && stompClient.ws.url) {
                    transportUrl = stompClient.ws.url;
                } else if (stompClient._transport && stompClient._transport.url) {
                    transportUrl = stompClient._transport.url;
                } else if (frame && frame.headers && frame.headers['session']) {
                    // 尝试从 frame headers 获取
                    mySessionId = frame.headers['session'];
                    console.log("🔄 [连接] WebSocket 连接成功，从 headers 获取 sessionId=" + mySessionId);
                } else {
                    // 如果都获取不到，生成一个临时 ID（用于调试）
                    mySessionId = 'temp_' + Date.now();
                    console.warn("🔄 [连接] 无法获取 Session ID，使用临时 ID: " + mySessionId);
                }
                
                if (transportUrl && !mySessionId) {
                    const urlParts = transportUrl.split('/');
                    if (urlParts.length >= 2) {
                        mySessionId = urlParts[urlParts.length - 2];
                        console.log("🔄 [连接] WebSocket 连接成功，从 URL 获取 sessionId=" + mySessionId);
                    } else {
                        mySessionId = 'temp_' + Date.now();
                        console.warn("🔄 [连接] URL 格式异常，使用临时 ID: " + mySessionId);
                    }
                }
                
                if (!mySessionId) {
                    mySessionId = 'temp_' + Date.now();
                    console.warn("🔄 [连接] 无法获取 Session ID，使用临时 ID: " + mySessionId);
                }
            } catch (e) {
                console.error("获取 Session ID 失败:", e);
                // 即使获取 Session ID 失败，也继续连接流程（使用临时 ID）
                mySessionId = 'temp_' + Date.now();
                console.warn("🔄 [连接] 获取 Session ID 异常，使用临时 ID: " + mySessionId);
            }

            // 确保 mySessionId 已设置
            if (!mySessionId) {
                console.error("🔄 [连接] ❌ mySessionId 未设置，无法订阅消息队列");
                if (onConnectedCallback) onConnectedCallback();
                return;
            }
            
            // 清理旧的订阅
            if (messageSubscriptions.errors) {
                try { messageSubscriptions.errors.unsubscribe(); } catch (e) {}
            }
            if (messageSubscriptions.roomCreated) {
                try { messageSubscriptions.roomCreated.unsubscribe(); } catch (e) {}
            }
            if (messageSubscriptions.gameStart) {
                try { messageSubscriptions.gameStart.unsubscribe(); } catch (e) {}
            }
            if (messageSubscriptions.playerInfo) {
                try { messageSubscriptions.playerInfo.unsubscribe(); } catch (e) {}
            }
            if (messageSubscriptions.roomState) {
                try { messageSubscriptions.roomState.unsubscribe(); } catch (e) {}
            }
            
            // 保存当前 sessionId，用于验证订阅路径
            const subscriptionSessionId = mySessionId;
            console.log("🔄 [连接] 开始订阅消息队列，sessionId=" + subscriptionSessionId);
            messageSubscriptions.errors = stompClient.subscribe(`/user/${subscriptionSessionId}/queue/errors`, onErrorMessage);
            messageSubscriptions.roomCreated = stompClient.subscribe(`/user/${subscriptionSessionId}/queue/room-created`, (payload) => {
                console.log("🔄 [订阅] 收到 room-created 消息，订阅 sessionId=" + subscriptionSessionId + ", 当前 sessionId=" + mySessionId + ", payload=" + (payload ? (payload.body ? payload.body.substring(0, 100) : 'no body') : 'null'));
                if (mySessionId !== subscriptionSessionId) {
                    console.warn("🔄 [订阅] ⚠️ sessionId 不匹配：订阅时=" + subscriptionSessionId + ", 当前=" + mySessionId);
                }
                onRoomCreated(payload);
            });
            messageSubscriptions.gameStart = stompClient.subscribe(`/user/${subscriptionSessionId}/queue/game-start`, (payload) => {
                console.log("🔄 [订阅] 收到 game-start 消息，订阅 sessionId=" + subscriptionSessionId + ", 当前 sessionId=" + mySessionId);
                if (mySessionId !== subscriptionSessionId) {
                    console.warn("🔄 [订阅] ⚠️ sessionId 不匹配：订阅时=" + subscriptionSessionId + ", 当前=" + mySessionId + "，可能收到错误身份信息");
                }
                onGameStart(payload);
            });
            messageSubscriptions.playerInfo = stompClient.subscribe(`/user/${subscriptionSessionId}/queue/player-info`, onPlayerInfo);
            messageSubscriptions.roomState = stompClient.subscribe(`/user/${subscriptionSessionId}/queue/room-state`, onRoomStateUpdate);
            
            console.log("🔄 [连接] ✅ 已订阅所有消息队列，sessionId=" + subscriptionSessionId);

            // 连接成功后更新重连按钮
            updateReconnectButton();
            schedulePreferredRoleUpdate();

            if (isReconnect) {
                const storedRoomValue = localStorage.getItem('avalonRoomNumber');
                const storedPlayerId = localStorage.getItem('avalonPlayerId');
                myPlayerId = storedPlayerId;
                let actualRoomNumber = null;

                if (storedRoomValue) {
                    try {
                        const parsedRoom = JSON.parse(storedRoomValue);
                        if (parsedRoom && typeof parsedRoom.roomNumber === 'string') {
                            actualRoomNumber = parsedRoom.roomNumber;
                            localStorage.setItem('avalonRoomNumber', actualRoomNumber);
                        } else if (typeof storedRoomValue === 'string') {
                            actualRoomNumber = storedRoomValue;
                        }
                    } catch (e) {
                        actualRoomNumber = storedRoomValue;
                    }
                }

                if (actualRoomNumber && storedPlayerId) {
                    console.log("🔄 [重连] 开始重连流程: roomNumber=" + actualRoomNumber + ", playerId=" + storedPlayerId);
                    
                    // 恢复 currentRoom 信息（如果之前有）
                    if (!currentRoom || currentRoom.roomNumber !== actualRoomNumber) {
                        currentRoom = { roomNumber: actualRoomNumber };
                    }
                    
                    joinAndSubscribe({ roomNumber: actualRoomNumber });
                    // 延迟发送重连请求，确保订阅已建立（增加延迟时间，确保订阅完全建立）
                    setTimeout(() => {
                        if (stompClient && stompClient.connected) {
                            console.log("🔄 [重连] 发送重连请求: roomNumber=" + actualRoomNumber + ", playerId=" + storedPlayerId);
                            stompClient.send("/app/rejoin-with-id", {}, JSON.stringify(buildRejoinPayload(actualRoomNumber, storedPlayerId)));
                        } else {
                            console.error("🔄 [重连] ❌ 连接未建立，无法发送重连请求");
                        }
                    }, 300); // 增加延迟到 300ms，确保订阅已建立
                } else {
                    console.warn("🔄 [重连] 缺少房间或玩家信息: roomNumber=" + actualRoomNumber + ", playerId=" + storedPlayerId);
                    // 重连失败时，如果当前在游戏页面，不要跳转到首页，保持当前页面
                    if (!pages.identity.classList.contains('active') && 
                        !pages.mission.classList.contains('active') && 
                        !pages.voting.classList.contains('active') &&
                        !pages.missionResult.classList.contains('active')) {
                        showPage('home');
                    }
                    if (onConnectedCallback) onConnectedCallback();
                    hideLoader();
                }
            } else {
                if (onConnectedCallback) onConnectedCallback();
            }
        }, (error) => {
            // 连接失败，清除连接标志
            isConnecting = false;
            console.error("连接失败:", error);
            stompClient = null;
            if (!isReconnect) {
                disconnectCleanup();
            }
            hideLoader();
            updateReconnectButton();
            if (isReconnect) {
                // 重连失败时，不要跳转到首页，保持在当前页面
                const storedRoomNumber = localStorage.getItem('avalonRoomNumber');
                const storedPlayerId = localStorage.getItem('avalonPlayerId');
                if (storedRoomNumber && storedPlayerId) {
                    // 如果有房间信息，保持在当前页面，显示错误
                    console.warn("🔄 [连接失败] 重连失败，但保留房间信息，保持在当前页面");
                    // 不显示错误，让用户手动点击重连按钮
                } else {
                    showError('home-error', '无法自动重连，请手动加入');
                }
                // 不要删除房间信息，以便手动重连
            } else {
                showError('home-error', '无法连接，请检查网络或刷新');
            }
        });
    } catch (e) {
        // 创建连接异常，清除连接标志
        isConnecting = false;
        console.error("创建连接失败:", e);
        if (isReconnect) {
            // 重连失败时，不要显示错误或跳转，让用户手动重连
            console.warn("🔄 [创建连接失败] 重连时连接失败，保持在当前页面");
        } else {
            showError('home-error', '创建连接失败');
            disconnectCleanup();
        }
        hideLoader();
    }
}

function disconnectCleanup() {
    if (roomSubscription) {
        try {
            roomSubscription.unsubscribe();
        } catch (e) {}
        roomSubscription = null;
    }
    // 清理所有消息订阅
    if (messageSubscriptions.errors) {
        try { messageSubscriptions.errors.unsubscribe(); } catch (e) {}
        messageSubscriptions.errors = null;
    }
    if (messageSubscriptions.roomCreated) {
        try { messageSubscriptions.roomCreated.unsubscribe(); } catch (e) {}
        messageSubscriptions.roomCreated = null;
    }
    if (messageSubscriptions.gameStart) {
        try { messageSubscriptions.gameStart.unsubscribe(); } catch (e) {}
        messageSubscriptions.gameStart = null;
    }
    if (messageSubscriptions.playerInfo) {
        try { messageSubscriptions.playerInfo.unsubscribe(); } catch (e) {}
        messageSubscriptions.playerInfo = null;
    }
    if (messageSubscriptions.roomState) {
        try { messageSubscriptions.roomState.unsubscribe(); } catch (e) {}
        messageSubscriptions.roomState = null;
    }
    currentRoom = null;
    mySelectedSeat = 0;
    // 重置加入房间标志
    isJoiningRoom = false;
    lastJoinRoomAttempt = null;
    if (seatGrid) seatGrid.innerHTML = '';
    if (roomStatusText) roomStatusText.textContent = '未在房间内';
    if (btnReady) {
        btnReady.disabled = true;
        btnReady.textContent = '准备';
        btnReady.classList.remove('btn-secondary');
        btnReady.classList.add('btn-primary');
        btnReady.dataset.isReady = 'false';
    }
    if (roomInfoNumber) roomInfoNumber.textContent = '';
    hideError('room-error');
}

function disconnect(clearStoredData = false) {
    const wasConnected = stompClient && stompClient.connected;
    disconnectCleanup();
    if (wasConnected && stompClient) {
        try {
            stompClient.disconnect(() => {
                stompClient = null;
                updateReconnectButton();
            }, {});
        } catch (e) {
            console.warn("断开连接失败:", e);
            stompClient = null;
            updateReconnectButton();
        }
    } else {
        stompClient = null;
        updateReconnectButton();
    }
    if (clearStoredData) {
        localStorage.removeItem('avalonRoomNumber');
        localStorage.removeItem('avalonPlayerId');
        localStorage.removeItem('avalonIdentityData');
        localStorage.removeItem('avalonGameInProgress');
        myPlayerId = null;
        updateReconnectButton();
    }
    showPage('home');
    hideLoader();
}

function onErrorMessage(payload) {
    hideLoader();
    if (!payload || !payload.body) return;
    try {
        const errorData = JSON.parse(payload.body);
        const errorMessage = errorData.error || "未知错误";

        console.log("🔄 [错误消息] 收到错误: " + errorMessage);

        if (errorMessage.includes("您已被房主移出房间")) {
            showError('home-error', errorMessage);
            disconnect(true);
            return;
        }

        // 房间不存在或已过期的特殊处理
        if (errorMessage.includes("房间不存在") || errorMessage.includes("房间已过期")) {
            console.warn("🔄 [错误消息] 房间不存在或已过期，清理本地数据");
            // 清理本地存储，但保持在当前页面显示错误
            localStorage.removeItem('avalonRoomNumber');
            localStorage.removeItem('avalonPlayerId');
            localStorage.removeItem('avalonIdentityData');
            localStorage.removeItem('avalonGameInProgress');
            myPlayerId = null;
            currentRoom = null;
            updateReconnectButton();
            
            // 如果当前在游戏页面，显示错误但不跳转
            if (pages.identity && pages.identity.classList.contains('active')) {
                showError('identity-error', errorMessage + "，请返回首页重新加入房间");
            } else if (pages.mission && pages.mission.classList.contains('active')) {
                showError('mission-error', errorMessage + "，请返回首页重新加入房间");
            } else if (pages.voting && pages.voting.classList.contains('active')) {
                showError('voting-error', errorMessage + "，请返回首页重新加入房间");
            } else {
                // 其他页面跳转到首页
                showError('home-error', errorMessage);
                showPage('home');
            }
            return;
        }

        // 根据当前页面显示错误
        if (pages.identity && pages.identity.classList.contains('active')) {
            showError('identity-error', errorMessage);
        } else if (pages.mission && pages.mission.classList.contains('active')) {
            showError('mission-error', errorMessage);
        } else if (pages.voting && pages.voting.classList.contains('active')) {
            showError('voting-error', errorMessage);
        } else if (errorMessage.includes("房间已存在") || errorMessage.includes("房间已满")) {
            showError('home-error', errorMessage);
        } else if (errorMessage.includes("座位") && pages.room && pages.room.classList.contains('active')) {
            showError('room-error', errorMessage);
        } else if (pages.room && pages.room.classList.contains('active')) {
            showError('room-error', errorMessage);
        } else {
            if (homeError) showError('home-error', errorMessage);
        }
    } catch (e) {
        console.error("处理错误消息失败:", e);
        if (homeError) showError('home-error', '处理服务器消息失败');
    }
}

function onRoomCreated(payload) {
    // 清除创建房间超时定时器
    if (createRoomTimeout) {
        clearTimeout(createRoomTimeout);
        createRoomTimeout = null;
    }
    
    hideLoader();
    if (!payload || !payload.body) return;
    try {
        const roomData = JSON.parse(payload.body);
        console.log("🔄 [房间创建] 收到 room-created 消息: roomNumber=" + roomData.roomNumber + ", mySessionId=" + mySessionId);
        if (mySessionId && roomData.players && roomData.players[mySessionId]) {
            myPlayerId = roomData.players[mySessionId].playerId;
            localStorage.setItem('avalonPlayerId', myPlayerId);
            localStorage.setItem('avalonRoomNumber', roomData.roomNumber);
            console.log("🔄 [房间创建] ✅ 找到自己的玩家信息: playerId=" + myPlayerId + ", sessionId=" + mySessionId);
            // 重置加入房间标志
            isJoiningRoom = false;
            lastJoinRoomAttempt = null;
        } else {
            console.warn("🔄 [房间创建] ⚠️ 未找到自己的玩家信息: mySessionId=" + mySessionId + ", players=" + Object.keys(roomData.players || {}).join(', '));
        }
        joinAndSubscribe(roomData);
        onRoomStateUpdate({ body: JSON.stringify(roomData) });
    } catch (e) {
        console.error("处理房间创建消息失败:", e);
    }
}

function onRoomStateUpdate(payload) {
    hideLoader();
    updateReconnectButton();
    if (!payload || !payload.body) return;

    try {
        const roomState = JSON.parse(payload.body);
        let shouldUpdateCurrentRoom = false;

        const isCurrentRoom = currentRoom && roomState.roomNumber === currentRoom.roomNumber;
        const isFirstRoomState = !currentRoom && roomState.roomNumber;
        
        // 检查是否是重连后的状态更新（通过 localStorage 中的房间号判断）
        const storedRoomNumber = localStorage.getItem('avalonRoomNumber');
        const isReconnectState = storedRoomNumber && storedRoomNumber === roomState.roomNumber && !currentRoom;

        console.log("🔄 [房间状态更新] roomNumber=" + roomState.roomNumber + ", isCurrentRoom=" + isCurrentRoom + 
                   ", isFirstRoomState=" + isFirstRoomState + ", isReconnectState=" + isReconnectState + 
                   ", gameStarted=" + roomState.gameStarted + ", currentPage=" + 
                   Object.keys(pages).find(key => pages[key] && pages[key].classList.contains('active')));

        // 如果游戏已重置（从未开始状态或从已开始变为未开始），跳转到房间页面
        if (isCurrentRoom && !roomState.gameStarted) {
            const wasGameStarted = currentRoom && currentRoom.gameStarted;
            // 如果游戏刚刚被重置（从已开始变为未开始），或当前不在房间页面，跳转到房间页面
            if (wasGameStarted || !pages.room.classList.contains('active')) {
                showPage('room');
            }
            shouldUpdateCurrentRoom = true;
        } else if (pages.room.classList.contains('active')) {
            if (isCurrentRoom || isFirstRoomState || isReconnectState) {
                shouldUpdateCurrentRoom = true;
                // 如果游戏已开始，应该跳转到身份页面（重连后可能还在房间页面）
                if (roomState.gameStarted) {
                    const storedIdentityData = localStorage.getItem('avalonIdentityData');
                    if (storedIdentityData) {
                        try {
                            const identityData = JSON.parse(storedIdentityData);
                            if (identityCard) {
                                identityCard.className = `card ${identityData.camp}`;
                                identityImage.src = identityData.imageUrl;
                                identityName.textContent = identityData.identity;
                                seenTitle.textContent = identityData.seenTitle;
                                seenPlayers.textContent = identityData.seenPlayers.length > 0 ? identityData.seenPlayers.join(', ') : '—';
                                seenSubtitle.textContent = identityData.seenSubtitle;
                                leaderSeatNumber.textContent = identityData.leaderSeat;
                            }
                            localStorage.setItem('avalonGameInProgress', 'true');
                            showPage('identity');
                            console.log("🔄 [房间状态更新] ✅ 游戏已开始，从房间页面跳转到身份页面");
                        } catch (e) {
                            console.error("解析身份数据失败:", e);
                        }
                    } else {
                        // 没有身份数据，但游戏已开始，等待后端重新发送身份信息
                        console.warn("🔄 [房间状态更新] 游戏已开始但没有身份数据，等待后端重新发送身份信息");
                        // 先跳转到身份页面等待
                        showPage('identity');
                    }
                }
            } else {
                return;
            }
        } else if (pages.identity.classList.contains('active') && roomState.gameStarted) {
            // 身份页面且游戏已开始，需要处理任务状态更新
            shouldUpdateCurrentRoom = true;
        } else if (pages.mission.classList.contains('active') && roomState.gameStarted) {
            // 任务页面，需要处理任务状态更新
            shouldUpdateCurrentRoom = true;
        } else if (pages.voting.classList.contains('active') && roomState.gameStarted) {
            // 投票页面，需要处理任务状态更新
            shouldUpdateCurrentRoom = true;
        } else if (pages.missionResult.classList.contains('active') && roomState.gameStarted) {
            // 结果页面，需要处理任务状态更新
            shouldUpdateCurrentRoom = true;
        } else if (isCurrentRoom || isFirstRoomState || isReconnectState) {
            // 如果是当前房间的状态更新，或者重连后的状态更新，即使不在游戏相关页面，也应该更新
            // 重连后如果在首页或房间页面，需要根据游戏状态跳转到相应页面
            if (isReconnectState && (pages.home.classList.contains('active') || pages.room.classList.contains('active'))) {
                console.log("🔄 [房间状态更新] 检测到重连状态，当前页面: " + 
                           (pages.home.classList.contains('active') ? 'home' : 'room') + 
                           ", 游戏状态: gameStarted=" + roomState.gameStarted);
                
                if (roomState.gameStarted) {
                    // 游戏已开始，应该跳转到身份页面
                    const storedIdentityData = localStorage.getItem('avalonIdentityData');
                    if (storedIdentityData) {
                        try {
                            const identityData = JSON.parse(storedIdentityData);
                            if (identityCard) {
                                identityCard.className = `card ${identityData.camp}`;
                                identityImage.src = identityData.imageUrl;
                                identityName.textContent = identityData.identity;
                                seenTitle.textContent = identityData.seenTitle;
                                seenPlayers.textContent = identityData.seenPlayers.length > 0 ? identityData.seenPlayers.join(', ') : '—';
                                seenSubtitle.textContent = identityData.seenSubtitle;
                                leaderSeatNumber.textContent = identityData.leaderSeat;
                            }
                            // 确保游戏进行中标记存在
                            localStorage.setItem('avalonGameInProgress', 'true');
                            showPage('identity');
                            console.log("🔄 [房间状态更新] ✅ 重连后从首页/房间页面跳转到身份页面");
                        } catch (e) {
                            console.error("解析身份数据失败:", e);
                        }
                    } else {
                        // 没有身份数据，但游戏已开始，说明可能是长时间断线后身份数据丢失
                        // 这种情况下，后端应该会重新发送身份信息（通过 onGameStart）
                        console.warn("🔄 [房间状态更新] 重连后没有身份数据，等待后端重新发送身份信息");
                        // 保持在当前页面，等待后端发送身份信息
                        // 但不要跳转到首页，保持在身份页面等待
                        if (pages.home.classList.contains('active')) {
                            showPage('identity');
                            console.log("🔄 [房间状态更新] 在首页但游戏已开始，跳转到身份页面等待身份信息");
                        }
                    }
                } else {
                    // 游戏未开始，跳转到房间页面
                    showPage('room');
                    console.log("🔄 [房间状态更新] 重连后跳转到房间页面（游戏未开始）");
                }
            }
            shouldUpdateCurrentRoom = true;
        } else {
            // 如果不在任何已知页面且不是当前房间的更新，不处理
            return;
        }

        // 如果游戏已开始，总是更新房间状态（用于任务状态处理）
        if (roomState.gameStarted && (isCurrentRoom || isReconnectState)) {
            shouldUpdateCurrentRoom = true;
        }

        if (!shouldUpdateCurrentRoom) {
            return;
        }

        currentRoom = roomState;
        
        // 更新玩家信息：优先通过 sessionId 查找，如果找不到则遍历所有玩家
        let myStateFromServer = null;
        if (mySessionId && roomState.players && roomState.players[mySessionId]) {
            myStateFromServer = roomState.players[mySessionId];
        } else if (myPlayerId && roomState.players) {
            // 如果通过 sessionId 找不到，尝试通过 playerId 查找
            myStateFromServer = Object.values(roomState.players).find(p => p.playerId === myPlayerId);
        }
        
        // 如果找不到自己的玩家信息，且当前在房间页面，尝试重新加入房间
        if (!myStateFromServer && pages.room && pages.room.classList.contains('active') && isCurrentRoom) {
            const storedRoomNumber = localStorage.getItem('avalonRoomNumber');
            const storedPlayerId = localStorage.getItem('avalonPlayerId');
            
            if (storedRoomNumber === roomState.roomNumber && storedPlayerId) {
                // 有 localStorage 中的房间和玩家信息，尝试重新加入房间
                console.warn("🔄 [房间状态更新] 在房间页面但找不到自己的玩家信息，尝试重新加入房间: roomNumber=" + storedRoomNumber + ", playerId=" + storedPlayerId);
                if (stompClient && stompClient.connected) {
                    // 先确保订阅了房间主题
                    if (!roomSubscription || !roomSubscription.active) {
                        joinAndSubscribe({ roomNumber: storedRoomNumber });
                    }
                    // 延迟发送重连请求，确保订阅已建立
                    setTimeout(() => {
                        if (stompClient && stompClient.connected) {
                            console.log("🔄 [房间状态更新] 发送重连请求: roomNumber=" + storedRoomNumber + ", playerId=" + storedPlayerId);
                            stompClient.send("/app/rejoin-with-id", {}, JSON.stringify(buildRejoinPayload(storedRoomNumber, storedPlayerId)));
                        }
                    }, 300);
                }
            } else if (storedRoomNumber === roomState.roomNumber && !storedPlayerId) {
                // 有房间号但没有玩家ID，说明是新加入的房间，需要发送 join-room 请求
                // 防止重复发送请求（3秒内只发送一次）
                const now = Date.now();
                const joinKey = roomState.roomNumber + '_' + mySessionId;
                if (!isJoiningRoom && (!lastJoinRoomAttempt || lastJoinRoomAttempt.key !== joinKey || now - lastJoinRoomAttempt.time > 3000)) {
                    console.warn("🔄 [房间状态更新] 在房间页面但找不到自己的玩家信息，且没有玩家ID，尝试发送 join-room 请求: roomNumber=" + storedRoomNumber + ", sessionId=" + mySessionId);
                    if (stompClient && stompClient.connected) {
                        isJoiningRoom = true;
                        lastJoinRoomAttempt = { key: joinKey, time: now };
                        // 先确保订阅了房间主题
                        if (!roomSubscription || !roomSubscription.active) {
                            joinAndSubscribe({ roomNumber: storedRoomNumber });
                        }
                        // 延迟发送加入房间请求，确保订阅已建立
                        setTimeout(() => {
                            if (stompClient && stompClient.connected) {
                                try {
                                    console.log("🔄 [房间状态更新] 发送加入房间请求: roomNumber=" + storedRoomNumber + ", sessionId=" + mySessionId);
                                    stompClient.send("/app/join-room", {}, JSON.stringify(buildJoinPayload(storedRoomNumber)));
                                    console.log("🔄 [房间状态更新] ✅ 加入房间请求已发送");
                                    // 设置一个超时检查，如果3秒后仍然找不到自己的玩家信息，允许重试
                                    setTimeout(() => {
                                        isJoiningRoom = false;
                                        // 如果3秒后仍然找不到自己的玩家信息，清除 lastJoinRoomAttempt 以允许重试
                                        if (!myPlayerId) {
                                            console.warn("🔄 [房间状态更新] ⚠️ 发送加入房间请求后3秒仍未找到玩家信息，允许重试");
                                            lastJoinRoomAttempt = null;
                                        }
                                    }, 3000);
                                } catch (e) {
                                    console.error("🔄 [房间状态更新] ❌ 发送加入房间请求失败:", e);
                                    isJoiningRoom = false;
                                    // 发送失败，立即允许重试
                                    lastJoinRoomAttempt = null;
                                }
                            } else {
                                console.warn("🔄 [房间状态更新] ⚠️ 连接未建立，无法发送加入房间请求: connected=" + (stompClient ? stompClient.connected : false));
                                isJoiningRoom = false;
                            }
                        }, 500); // 增加延迟到500ms，确保连接完全建立
                    }
                } else {
                    console.log("🔄 [房间状态更新] 跳过重复的加入房间请求: isJoiningRoom=" + isJoiningRoom + ", lastAttempt=" + (lastJoinRoomAttempt ? (now - lastJoinRoomAttempt.time) + "ms ago" : "null"));
                }
            } else {
                // 没有 localStorage 中的房间或玩家信息，说明这是一个新连接，需要手动加入房间
                console.warn("🔄 [房间状态更新] 在房间页面但找不到自己的玩家信息，且没有 localStorage 信息，需要手动加入房间");
                console.warn("🔄 [房间状态更新] 当前 sessionId=" + mySessionId + ", 房间中的玩家 sessionId: " + Object.keys(roomState.players || {}).join(', '));
            }
        }
        
        if (myStateFromServer && myStateFromServer.playerId) {
            // 找到了自己的玩家信息，重置加入房间标志
            isJoiningRoom = false;
            lastJoinRoomAttempt = null;
            
            if (!myPlayerId || myPlayerId !== myStateFromServer.playerId) {
                myPlayerId = myStateFromServer.playerId;
                localStorage.setItem('avalonPlayerId', myPlayerId);
            }
            // 如果找到了玩家但 mySessionId 不匹配，更新 mySessionId
            if (myStateFromServer.sessionId && myStateFromServer.sessionId !== mySessionId) {
                const oldSessionId = mySessionId;
                console.log("🔄 [房间状态更新] 更新 sessionId: " + oldSessionId + " -> " + myStateFromServer.sessionId);
                mySessionId = myStateFromServer.sessionId;
                
                // 如果 sessionId 更新了，且游戏已开始，重新订阅 game-start 队列以确保能收到身份信息
                if (roomState.gameStarted && stompClient && stompClient.connected) {
                    console.log("🔄 [房间状态更新] sessionId 已更新，重新订阅 game-start 队列: 旧 sessionId=" + (messageSubscriptions.gameStart ? '已存在' : 'null') + ", 新 sessionId=" + mySessionId);
                    try {
                        // 取消旧订阅（如果存在）
                        if (messageSubscriptions.gameStart) {
                            try {
                                messageSubscriptions.gameStart.unsubscribe();
                                console.log("🔄 [房间状态更新] ✅ 已取消旧的 game-start 订阅");
                            } catch (e) {
                                console.warn("🔄 [房间状态更新] 取消旧订阅失败（可能已自动取消）:", e);
                            }
                        }
                        // 使用新的 sessionId 重新订阅
                        messageSubscriptions.gameStart = stompClient.subscribe(`/user/${mySessionId}/queue/game-start`, (payload) => {
                            console.log("🔄 [订阅] 收到 game-start 消息，sessionId=" + mySessionId);
                            onGameStart(payload);
                        });
                        console.log("🔄 [房间状态更新] ✅ 已重新订阅 game-start 队列: /user/" + mySessionId + "/queue/game-start");
                    } catch (e) {
                        console.error("🔄 [房间状态更新] 重新订阅 game-start 队列失败:", e);
                    }
                }
            }
            localStorage.setItem('avalonRoomNumber', roomState.roomNumber);
            console.log("🔄 [房间状态更新] 更新玩家信息: playerId=" + myPlayerId + ", sessionId=" + mySessionId);
            
        // 如果游戏已开始，且玩家有身份信息，但没有 localStorage 中的身份数据，尝试从房间状态恢复
        if (roomState.gameStarted && myStateFromServer && myStateFromServer.identity && !localStorage.getItem('avalonIdentityData')) {
            console.log("🔄 [房间状态更新] 检测到玩家有身份信息但没有 localStorage 数据，尝试从房间状态恢复: identity=" + myStateFromServer.identity);
            // 注意：从房间状态中只能获取到身份名称，无法获取完整的身份信息（如 seenPlayers、seenTitle 等）
            // 所以这里需要请求后端重新发送完整的身份信息
            // 确保订阅了 game-start 队列
            if (stompClient && stompClient.connected && mySessionId) {
                console.log("🔄 [房间状态更新] 游戏已开始但缺少身份数据，确保订阅了 game-start 队列并请求后端重新发送");
                // 重新订阅 game-start 队列（以防万一）
                try {
                    // 取消旧订阅（如果存在）
                    if (messageSubscriptions.gameStart) {
                        try {
                            messageSubscriptions.gameStart.unsubscribe();
                            console.log("🔄 [房间状态更新] ✅ 已取消旧的 game-start 订阅");
                        } catch (e) {
                            console.warn("🔄 [房间状态更新] 取消旧订阅失败（可能已自动取消）:", e);
                        }
                    }
                    // 使用当前的 sessionId 重新订阅
                    messageSubscriptions.gameStart = stompClient.subscribe(`/user/${mySessionId}/queue/game-start`, (payload) => {
                        console.log("🔄 [订阅] 收到 game-start 消息，sessionId=" + mySessionId);
                        onGameStart(payload);
                    });
                    console.log("🔄 [房间状态更新] ✅ 已重新订阅 game-start 队列: /user/" + mySessionId + "/queue/game-start");
                } catch (e) {
                    console.error("🔄 [房间状态更新] 重新订阅 game-start 队列失败:", e);
                }
                // 延迟请求后端重新发送身份信息（给后端一些时间处理重连）
                setTimeout(() => {
                    if (stompClient && stompClient.connected && myPlayerId && currentRoom && currentRoom.roomNumber) {
                        console.log("🔄 [房间状态更新] 请求后端重新发送身份信息: playerId=" + myPlayerId + ", roomNumber=" + currentRoom.roomNumber);
                        stompClient.send("/app/rejoin-with-id", {}, JSON.stringify(buildRejoinPayload(currentRoom.roomNumber, myPlayerId)));
                    }
                }, 500);
            }
        }
        }
        
        // 更新任务历史显示
        updateMissionHistory(roomState);
        updateLakeLadyUI(roomState);
        
        // 更新人员配置说明（只要在房间页面就更新，不管是否激活）
        updatePlayerConfigInfo(roomState);

        const hostPlayerId = currentRoom.hostPlayerId;
        const amIHost = (myPlayerId && myPlayerId === hostPlayerId);

        seatGrid.innerHTML = '';
        let readyCount = 0;
        const playersMap = roomState.players || {};
        const takenSeats = new Set();
        Object.values(playersMap).forEach(p => {
            if (p.seatNumber > 0) takenSeats.add(p.seatNumber);
        });
        const totalSeats = Number.isInteger(roomState.totalPlayers) ? roomState.totalPlayers : 0;

        for (let i = 1; i <= totalSeats; i++) {
            const seat = document.createElement('div');
            seat.className = 'seat';
            seat.dataset.number = i;
            const playerOnSeat = Object.values(playersMap).find(p => p.seatNumber === i);

            let seatText = `<span class="seat-number">${i}</span>`;
            let hostIcon = '';
            let hostActions = '';

            if (playerOnSeat) {
                seat.classList.add('taken');
                if (playerOnSeat.sessionId === mySessionId) {
                    seat.classList.add('selected');
                    mySelectedSeat = i;
                }
                if (playerOnSeat.ready) {
                    seat.classList.add('ready');
                    readyCount++;
                }
                if (playerOnSeat.playerId === hostPlayerId) {
                    hostIcon = '<span class="host-icon">👑</span>';
                }

                if (amIHost && playerOnSeat.playerId !== myPlayerId) {
                    hostActions = `<div class="host-actions">
                        <button class="host-btn transfer-btn" onclick="transferHost(event, '${playerOnSeat.playerId}', ${i})">转</button>
                        <button class="host-btn kick-btn" onclick="kickPlayer(event, '${playerOnSeat.playerId}', ${i})">踢</button>
                    </div>`;
                }

                seat.onclick = null;
                if (playerOnSeat.sessionId === mySessionId) {
                    seat.onclick = () => selectSeat(i);
                }
            } else {
                seat.onclick = () => selectSeat(i);
            }

            seat.innerHTML = hostIcon + seatText + hostActions;
            seatGrid.appendChild(seat);
        }

        // 查找当前玩家的状态
        let myState = null;
        if (mySessionId && playersMap[mySessionId]) {
            myState = playersMap[mySessionId];
        } else if (myPlayerId) {
            // 如果通过 sessionId 找不到，尝试通过 playerId 查找
            myState = Object.values(playersMap).find(p => p.playerId === myPlayerId);
        }
        
        if (myState && myState.seatNumber > 0) {
            btnReady.disabled = false;
            const isReady = myState.ready;
            lastReadyState = !!isReady;
            btnReady.textContent = isReady ? '取消准备' : '准备';
            btnReady.dataset.isReady = isReady ? 'true' : 'false';
            if (isReady) {
                btnReady.classList.remove('btn-primary');
                btnReady.classList.add('btn-secondary');
            } else {
                btnReady.classList.remove('btn-secondary');
                btnReady.classList.add('btn-primary');
            }
            console.log("🔄 [房间状态更新] 准备按钮已启用: seatNumber=" + myState.seatNumber + ", isReady=" + isReady);
        } else {
            btnReady.disabled = true;
            btnReady.textContent = '准备';
            btnReady.classList.remove('btn-secondary');
            btnReady.classList.add('btn-primary');
            btnReady.dataset.isReady = 'false';
            lastReadyState = false;
            if (pages.room && pages.room.classList.contains('active')) {
                console.warn("🔄 [房间状态更新] 准备按钮被禁用: mySessionId=" + mySessionId + ", myPlayerId=" + myPlayerId + ", myState=" + (myState ? "found" : "not found"));
            }
        }

        const currentPlayers = Object.keys(playersMap).length;
        const displayTotal = totalSeats > 0 ? totalSeats : '?';
        roomStatusText.textContent = `玩家: ${currentPlayers} / ${displayTotal} | 已准备: ${readyCount} / ${displayTotal}`;
        
        // 处理任务状态
        if (roomState.gameStarted) {
            const round = roomState.currentMissionRound || 0;
            const selectedMembers = roomState.selectedMissionMembers || [];
            const membersArray = Array.isArray(selectedMembers) ? selectedMembers : Array.from(selectedMembers);
            const votes = roomState.votes || {};
            const votingCompleted = roomState.missionVotingCompleted || false;
            const missionResult = roomState.missionResult;
            const myState = playersMap[mySessionId];
            const mySeat = myState ? myState.seatNumber : 0;
            const amIOnMission = mySeat > 0 && membersArray.includes(mySeat);
            
            console.log("处理任务状态:", {
                round,
                selectedMembers: membersArray,
                votingCompleted,
                missionResult,
                mySeat,
                amIOnMission,
                currentPage: Object.keys(pages).find(key => pages[key] && pages[key].classList.contains('active'))
            });
            
            // 如果任务投票已完成，自动返回身份页面（任务结果会在任务历史中显示）
            if (votingCompleted && missionResult) {
                console.log("任务投票已完成，返回身份页面");
                if (!pages.identity.classList.contains('active')) {
                    showPage('identity');
                }
            }
            // 如果已选择任务成员，只有上车的玩家显示投票页面
            else if (round > 0 && membersArray.length > 0) {
                if (amIOnMission) {
                    // 我是任务成员，显示投票页面
                    console.log("切换到投票页面（我是任务成员）");
                    if (!pages.voting.classList.contains('active')) {
                        showVotingPage();
                    } else {
                        // 如果已经在投票页面，刷新显示
                        showVotingPage();
                    }
                } else {
                    // 不是任务成员，保持在身份页面
                    console.log("保持在身份页面（我不是任务成员，等待投票结果）");
                    if (!pages.identity.classList.contains('active')) {
                        showPage('identity');
                    }
                }
            }
            // 如果任务已开始但未选择成员
            // 其他玩家保持在身份页面
            else if (round > 0) {
                console.log("任务已开始但未选择成员，轮次:", round);
                // 如果已经在投票页面，保持在投票页面（等待加入任务）
                if (pages.voting.classList.contains('active')) {
                    // 保持在投票页面，等待后端将玩家加入任务
                } else if (pages.missionResult.classList.contains('active')) {
                    // 如果在结果页面，保持在结果页面
                } else {
                    // 其他情况，保持在身份页面
                    if (!pages.identity.classList.contains('active')) {
                        showPage('identity');
                    }
                }
            }
        }
    } catch (e) {
        console.error("处理房间状态更新失败:", e);
    }
}

function onGameStart(payload) {
    hideLoader();
    if (!payload || !payload.body) return;
    try {
        const identityData = JSON.parse(payload.body);
        console.log("🔄 [游戏开始] 收到身份信息: identity=" + identityData.identity + ", seatNumber=" + (identityData.seatNumber || 'unknown') + ", sessionId=" + mySessionId);
        
        // 验证身份信息是否属于当前玩家
        if (currentRoom && currentRoom.players && currentRoom.players[mySessionId]) {
            const myState = currentRoom.players[mySessionId];
            if (myState.seatNumber > 0 && identityData.seatNumber && identityData.seatNumber !== myState.seatNumber) {
                console.warn("🔄 [游戏开始] ⚠️ 收到身份信息的座位号不匹配：收到=" + identityData.seatNumber + ", 我的座位号=" + myState.seatNumber + "，可能是上一轮的身份信息，忽略");
                return;
            }
        }
        
        identityCard.className = `card ${identityData.camp}`;
        identityImage.src = identityData.imageUrl;
        identityName.textContent = identityData.identity;
        seenTitle.textContent = identityData.seenTitle;
        seenPlayers.textContent = identityData.seenPlayers.length > 0 ? identityData.seenPlayers.join(', ') : '—';
        seenSubtitle.textContent = identityData.seenSubtitle;
        leaderSeatNumber.textContent = identityData.leaderSeat;

        try {
            localStorage.setItem('avalonIdentityData', JSON.stringify(identityData));
            localStorage.setItem('avalonGameInProgress', 'true');
            if (currentRoom && !localStorage.getItem('avalonRoomNumber')) {
                localStorage.setItem('avalonRoomNumber', currentRoom.roomNumber);
            }
            console.log("🔄 [游戏开始] ✅ 身份信息已保存到 localStorage");
        } catch (e) {
            console.error("存储身份信息失败:", e);
            showError('room-error', '无法保存身份信息');
        }
        
        // 无论当前在哪个页面，都跳转到身份页面
        showPage('identity');
        console.log("🔄 [游戏开始] ✅ 已跳转到身份页面");
        
        // 初始化任务历史显示
        if (currentRoom) {
            updateMissionHistory(currentRoom);
            updateLakeLadyUI(currentRoom);
        }
    } catch (e) {
        console.error("游戏开始失败:", e);
    }
}

function updatePlayerConfigInfo(roomState) {
    const configInfo = document.getElementById('player-config-info');
    const configIdentities = document.getElementById('config-identities');
    const configVisibility = document.getElementById('config-visibility');
    
    if (!configInfo || !configIdentities || !configVisibility) {
        console.warn('配置说明元素未找到');
        return;
    }
    
    // 优先使用 roomState 的 totalPlayers，如果没有则使用 currentRoom，再没有则隐藏
    const totalPlayers = (roomState && roomState.totalPlayers) || (currentRoom && currentRoom.totalPlayers) || 0;
    console.log('updatePlayerConfigInfo - totalPlayers:', totalPlayers, 'roomState:', roomState, 'currentRoom:', currentRoom);
    
    if (!totalPlayers || totalPlayers < 5 || totalPlayers > 12) {
        console.log('totalPlayers 不在有效范围内，隐藏配置说明，totalPlayers:', totalPlayers);
        configInfo.style.display = 'none';
        return;
    }
    
    // 显示配置说明
    configInfo.style.display = 'block';
    
    // 根据人数获取身份列表
    const IDENTITIES_MAP = {
        5: ["梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客"],
        6: ["梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣"],
        7: ["梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "奥伯伦"],
        8: ["梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", "莫德雷德的爪牙"],
        9: ["梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "莫德雷德"],
        10: ["梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "莫德雷德", "奥伯伦"],
        11: ["梅林", "派西维尔", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "兰斯洛特（蓝）", "莫德雷德", "莫甘娜", "刺客", "兰斯洛特（红）"],
        12: ["梅林", "派西维尔", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "兰斯洛特（蓝）", "莫德雷德", "莫甘娜", "刺客", "奥伯伦", "兰斯洛特（红）"]
    };
    
    const ROLE_INFO = {
        "梅林": { emoji: "🧙‍♂️", camp: "good", desc: "能看到所有坏人（莫德雷德除外）" },
        "派西维尔": { emoji: "🕵️‍♀️", camp: "good", desc: "能看到梅林和莫甘娜" },
        "亚瑟的忠臣": { emoji: "👑", camp: "good", desc: "没有特殊技能" },
        "莫甘娜": { emoji: "😈", camp: "evil", desc: "假扮梅林，能看到队友（奥伯伦除外）" },
        "刺客": { emoji: "🗡️", camp: "evil", desc: "刺杀梅林，能看到队友（奥伯伦除外）" },
        "莫德雷德的爪牙": { emoji: "👹", camp: "evil", desc: "坏人，能看到队友（奥伯伦除外）" },
        "莫德雷德": { emoji: "💀", camp: "evil", desc: "坏人领袖，梅林看不到，能看到队友（奥伯伦除外）" },
        "奥伯伦": { emoji: "👻", camp: "evil", desc: "坏人，与队友互不相识" },
        "兰斯洛特（蓝）": { emoji: "🤺", camp: "good", desc: "好人，只能看到红兰" },
        "兰斯洛特（红）": { emoji: "💔", camp: "evil", desc: "坏人，只能看到蓝兰（梅林和其他坏人可见）" }
    };
    
    const identities = IDENTITIES_MAP[totalPlayers] || [];
    
    // 统计身份数量（合并亚瑟的忠臣）
    const identityCount = {};
    identities.forEach(identity => {
        identityCount[identity] = (identityCount[identity] || 0) + 1;
    });
    
    // 按阵营分组
    const goodRoles = [];
    const evilRoles = [];
    
    Object.keys(identityCount).forEach(identity => {
        const roleInfo = ROLE_INFO[identity] || { emoji: "❓", camp: "unknown", desc: "" };
        const count = identityCount[identity];
        let displayName = count > 1 && identity === "亚瑟的忠臣" ? `${identity} ×${count}` : identity;
        // 简化兰斯洛特的显示
        if (identity === "兰斯洛特（蓝）") {
            displayName = "蓝兰";
        } else if (identity === "兰斯洛特（红）") {
            displayName = "红兰";
        }
        const item = {
            identity: identity,
            displayName: displayName,
            emoji: roleInfo.emoji,
            camp: roleInfo.camp,
            count: count
        };
        
        if (roleInfo.camp === "good") {
            goodRoles.push(item);
        } else if (roleInfo.camp === "evil") {
            evilRoles.push(item);
        }
    });
    
    // 显示身份列表（按阵营分块，不显示标签和图标）
    let identitiesHtml = '<div class="config-identities-list">';
    
    // 好人阵营
    goodRoles.forEach(item => {
        const campClass = "good-role";
        identitiesHtml += `<div class="config-identity-item ${campClass}">
            <span class="config-identity-name">${item.displayName}</span>
        </div>`;
    });
    
    // 坏人阵营
    evilRoles.forEach(item => {
        const campClass = "evil-role";
        identitiesHtml += `<div class="config-identity-item ${campClass}">
            <span class="config-identity-name">${item.displayName}</span>
        </div>`;
    });
    
    identitiesHtml += '</div>';
    configIdentities.innerHTML = identitiesHtml;
    
    // 显示可见性说明
    let visibilityHtml = '<div class="config-visibility-list">';
    const visibilityRules = [];
    
    if (identities.includes("梅林")) {
        const hasMordred = identities.includes("莫德雷德");
        visibilityRules.push(`<div class="config-visibility-item"><strong>梅林</strong>：能看到所有坏人${hasMordred ? "（莫德雷德除外）" : ""}</div>`);
    }
    if (identities.includes("派西维尔")) {
        visibilityRules.push(`<div class="config-visibility-item"><strong>派西维尔</strong>：能看到梅林和莫甘娜（分不清谁是梅林）</div>`);
    }
    if (identities.includes("奥伯伦")) {
        visibilityRules.push(`<div class="config-visibility-item"><strong>奥伯伦</strong>：与所有坏人互不相识</div>`);
    }
    if (identities.includes("莫德雷德")) {
        visibilityRules.push(`<div class="config-visibility-item"><strong>莫德雷德</strong>：梅林看不到他</div>`);
    }
    if (identities.includes("兰斯洛特（蓝）") && identities.includes("兰斯洛特（红）")) {
        visibilityRules.push(`<div class="config-visibility-item"><strong>蓝兰</strong>：只能看到红兰</div>`);
        visibilityRules.push(`<div class="config-visibility-item"><strong>红兰</strong>：只能看到蓝兰（梅林和其他坏人也能看到红兰）</div>`);
    }
    
    if (visibilityRules.length === 0) {
        visibilityHtml += '<div class="config-visibility-item">无特殊可见性规则</div>';
    } else {
        visibilityHtml += visibilityRules.join('');
    }
    visibilityHtml += '</div>';
    configVisibility.innerHTML = visibilityHtml;
    
    configInfo.style.display = 'block';
}

function updateMissionHistory(roomState) {
    if (!missionHistoryContent) return;
    
    const history = roomState.missionHistory || [];
    if (history.length === 0) {
        missionHistoryContent.textContent = '暂无任务记录';
        return;
    }
    
    let html = '';
    history.forEach(item => {
        const members = Array.isArray(item.members) ? item.members : Array.from(item.members || []);
        const membersText = members.sort((a, b) => a - b).join(', ');
        const resultText = item.result === 'success' ? '✅ 成功' : '❌ 失败';
        const resultColor = item.result === 'success' ? '#4CAF50' : '#f44336';
        html += `<div style="margin-bottom: 4px;">第 ${item.round} 轮: ${membersText} 号 → <span style="color: ${resultColor}; font-weight: bold;">${resultText}</span></div>`;
    });
    missionHistoryContent.innerHTML = html;
}

function updateLakeLadyUI(roomState) {
    if (!lakeLadySection || !currentRoom) return;
    
    // 8人以下不显示湖中仙女
    const totalPlayers = currentRoom.totalPlayers || 0;
    if (totalPlayers < 8) {
        lakeLadySection.style.display = 'none';
        return;
    }
    
    const myState = currentRoom.players ? currentRoom.players[mySessionId] : null;
    const mySeat = myState ? myState.seatNumber : 0;
    const lakeLadyHolderSeat = roomState.lakeLadyHolderSeat;
    const isMeHolder = mySeat > 0 && lakeLadyHolderSeat === mySeat;
    const lastInspectorSeat = roomState.lakeLadyLastInspectorSeat;
    const round = roomState.currentMissionRound || 0;
    const votingCompleted = roomState.missionVotingCompleted || false;
    const canUse = round >= 2 && round <= 4 && votingCompleted;
    const inspectionHistory = roomState.lakeLadyInspectionHistory || {};
    const hasInspected = inspectionHistory[round] !== undefined;
    const isLastInspector = hasInspected && lastInspectorSeat != null && lastInspectorSeat === mySeat;
    const shouldShow = (isMeHolder && canUse) || isLastInspector;
    
    if (!shouldShow) {
        lakeLadySection.style.display = 'none';
        if (lakeLadySubtitle) {
            lakeLadySubtitle.textContent = '选择查验玩家';
        }
        return;
    }

    lakeLadySection.style.display = 'block';
    if (lakeLadySubtitle) {
        let subtitleText = '选择查验玩家';
        if (hasInspected) {
            subtitleText = isLastInspector ? '查验结果' : '等待下一轮';
        }
        lakeLadySubtitle.textContent = subtitleText;
    }

    if (hasInspected) {
        lakeLadyInspectSection.style.display = 'none';
        if (isLastInspector) {
            lakeLadyResultSection.style.display = 'block';

            const targetSeat = inspectionHistory[round];
            const inspectionResult = roomState.lakeLadyInspectionResult || {};
            const actualCamp = inspectionResult[targetSeat] || 'unknown';
            const campText = actualCamp === 'good' ? '好人（蓝色）' : '坏人（红色）';

            lakeLadyInspectResult.textContent = `${targetSeat} 号：${campText}`;
        } else {
            lakeLadyResultSection.style.display = 'none';
        }
    } else {
        lakeLadyInspectSection.style.display = 'block';
        lakeLadyResultSection.style.display = 'none';

        lakeLadySeatGrid.innerHTML = '';
        const usedSeats = new Set(roomState.lakeLadyUsedSeats || []);

        for (let i = 1; i <= roomState.totalPlayers; i++) {
            const seat = document.createElement('div');
            seat.className = 'lake-lady-seat';
            seat.textContent = i;

            if (i === mySeat || usedSeats.has(i)) {
                seat.classList.add('disabled');
                seat.onclick = null;
            } else {
                seat.onclick = () => {
                    if (!stompClient || !stompClient.connected || !currentRoom) {
                        showError('identity-error', '连接已断开');
                        return;
                    }
                    hideError('identity-error');
                    showLoader("查验中...");
                    stompClient.send("/app/lake-lady-inspect", {}, JSON.stringify({
                        roomNumber: currentRoom.roomNumber,
                        seatNumber: i
                    }));
                };
            }

            lakeLadySeatGrid.appendChild(seat);
        }
    }
}

// 任务人数配置
const MISSION_SIZES = {
    5: [2, 3, 2, 3, 3],
    6: [2, 3, 4, 3, 4],
    7: [2, 3, 3, 4, 4],
    8: [3, 4, 4, 5, 5],
    9: [3, 4, 4, 5, 5],
    10: [3, 4, 4, 5, 5],
    11: [3, 4, 4, 5, 5],
    12: [3, 4, 4, 5, 5]
};

function getRequiredMissionSize(totalPlayers, round) {
    const sizes = MISSION_SIZES[totalPlayers];
    if (!sizes || round < 1 || round > sizes.length) return 0;
    return sizes[round - 1];
}

function showMissionPage() {
    if (!currentRoom) return;
    
    const round = currentRoom.currentMissionRound || 1;
    const requiredSize = getRequiredMissionSize(currentRoom.totalPlayers, round);
    const leaderSeat = currentRoom.currentLeaderSeat || 1;
    const selectedMembers = currentRoom.selectedMissionMembers || [];
    const selectedMembersSet = new Set(Array.isArray(selectedMembers) ? selectedMembers : Array.from(selectedMembers));
    
    missionRoundNumber.textContent = round;
    missionLeaderSeat.textContent = leaderSeat;
    missionRequiredSize.textContent = requiredSize;
    
    missionSeatGrid.innerHTML = '';
    hideError('mission-error');
    
    const playersMap = currentRoom.players || {};
    const totalSeats = currentRoom.totalPlayers || 0;
    const myState = playersMap[mySessionId];
    const mySeat = myState ? myState.seatNumber : 0;
    const currentCount = selectedMembersSet.size;
    
    // 显示当前已上车人数（如果已存在则更新，否则创建）
    let statusText = missionSeatGrid.previousElementSibling;
    if (!statusText || !statusText.classList.contains('prompt-box') || statusText.id !== 'mission-status-text') {
        statusText = document.createElement('div');
        statusText.id = 'mission-status-text';
        statusText.className = 'prompt-box';
        statusText.style.marginBottom = '10px';
        missionSeatGrid.parentNode.insertBefore(statusText, missionSeatGrid);
    }
    statusText.textContent = `已上车: ${currentCount} / ${requiredSize} 人`;
    
    for (let i = 1; i <= totalSeats; i++) {
        const seat = document.createElement('div');
        seat.className = 'seat';
        seat.dataset.number = i;
        
        const playerOnSeat = Object.values(playersMap).find(p => p.seatNumber === i);
        if (playerOnSeat) {
            seat.classList.add('taken');
            const isSelected = selectedMembersSet.has(i);
            if (isSelected) {
                seat.classList.add('selected');
            }
            
            let seatText = `<span class="seat-number">${i}</span>`;
            if (playerOnSeat.sessionId === mySessionId) {
                seatText += '<span style="font-size: 12px; display: block;">(我)</span>';
            }
            seat.innerHTML = seatText;
            
            // 只有自己可以点击自己的座位
            if (playerOnSeat.sessionId === mySessionId) {
                const currentIsOnMission = selectedMembersSet.has(i);
                seat.onclick = () => {
                    if (!stompClient || !stompClient.connected || !currentRoom) {
                        showError('mission-error', '连接已断开');
                        return;
                    }
                    const newState = !currentIsOnMission;
                    showLoader(newState ? "上车中..." : "下车中...");
                    stompClient.send("/app/toggle-mission-member", {}, JSON.stringify({
                        roomNumber: currentRoom.roomNumber,
                        joinMission: newState
                    }));
                };
            } else {
                seat.style.opacity = '0.6';
                seat.style.cursor = 'default';
            }
        } else {
            seat.innerHTML = `<span class="seat-number">${i}</span>`;
            seat.style.opacity = '0.3';
        }
        
        missionSeatGrid.appendChild(seat);
    }
    
    // 移除确认按钮，因为人数达到要求后自动开始投票
    if (btnConfirmMission) {
        btnConfirmMission.style.display = 'none';
    }
    
    showPage('mission');
}

function showVotingPage() {
    if (!currentRoom) return;
    
    const round = currentRoom.currentMissionRound || 1;
    const members = currentRoom.selectedMissionMembers || [];
    const membersArray = Array.isArray(members) ? [...members] : Array.from(members);
    const votes = currentRoom.votes || {};
    
    votingRoundNumber.textContent = round;
    const displayMembers = [...membersArray].sort((a, b) => a - b).join(', ');
    votingMembers.textContent = displayMembers || '待加入';
    
    const myState = currentRoom.players ? currentRoom.players[mySessionId] : null;
    const canVote = myState && membersArray.includes(myState.seatNumber);
    const hasVoted = votes[mySessionId] !== undefined;
    const votedCount = Object.keys(votes).length;
    const requiredSize = getRequiredMissionSize(currentRoom.totalPlayers || 0, round) || membersArray.length;
    const totalVoters = requiredSize;
    
    // 获取玩家身份信息
    let myIdentity = null;
    let myCamp = null;
    try {
        const storedIdentityData = localStorage.getItem('avalonIdentityData');
        if (storedIdentityData) {
            const identityData = JSON.parse(storedIdentityData);
            myIdentity = identityData.identity;
            myCamp = identityData.camp;
        }
    } catch (e) {
        console.error("读取身份信息失败:", e);
    }
    
    // 判断是否可以投票失败
    // 好人阵营（除了兰斯洛特（蓝））只能投成功
    // 坏人阵营和兰斯洛特（蓝）可以选择成功或失败
    const canVoteFail = myCamp === 'evil' || myIdentity === '兰斯洛特（蓝）';
    
    if (canVote && !hasVoted) {
        btnVoteSuccess.disabled = false;
        btnVoteFail.disabled = !canVoteFail; // 根据身份禁用失败按钮
        if (!canVoteFail) {
            btnVoteFail.style.opacity = '0.5';
            btnVoteFail.style.cursor = 'not-allowed';
            btnVoteFail.title = '好人阵营只能投任务成功';
        } else {
            btnVoteFail.style.opacity = '1';
            btnVoteFail.style.cursor = 'pointer';
            btnVoteFail.title = '';
        }
        votingStatus.textContent = '请选择任务结果';
    } else if (canVote && hasVoted) {
        btnVoteSuccess.disabled = true;
        btnVoteFail.disabled = true;
        votingStatus.textContent = `已投票，等待其他玩家 (${votedCount}/${totalVoters})`;
    } else {
        btnVoteSuccess.disabled = true;
        btnVoteFail.disabled = true;
        votingStatus.textContent = `等待任务成员投票 (${votedCount}/${totalVoters})`;
    }
    
    hideError('voting-error');
    showPage('voting');
}

function showMissionResultPage() {
    if (!currentRoom) return;
    
    const round = currentRoom.currentMissionRound || 1;
    const result = currentRoom.missionResult;
    
    resultRoundNumber.textContent = round;
    
    if (result === 'success') {
        missionResultDisplay.textContent = '✅ 任务成功';
        missionResultDisplay.style.color = '#4CAF50';
    } else if (result === 'fail') {
        missionResultDisplay.textContent = '❌ 任务失败';
        missionResultDisplay.style.color = '#f44336';
    } else {
        missionResultDisplay.textContent = '等待结果...';
    }
    
    showPage('missionResult');
}

if (btnStartMission) {
    btnStartMission.onclick = () => {
        console.log("点击上车做任务按钮");
        console.log("stompClient:", stompClient);
        console.log("stompClient.connected:", stompClient ? stompClient.connected : "stompClient is null");
        console.log("currentRoom:", currentRoom);
        
        // 如果连接断开，先尝试自动重连
        if (!stompClient || !stompClient.connected) {
            const storedRoomNumber = localStorage.getItem('avalonRoomNumber');
            const storedPlayerId = localStorage.getItem('avalonPlayerId');
            
            if (storedRoomNumber && storedPlayerId) {
                console.log("连接断开，尝试自动重连...");
                showLoader("连接中...");
                hideError('identity-error');
                
                connect(() => {
                    const roomNumber = (currentRoom && currentRoom.roomNumber) || storedRoomNumber;
                    if (roomNumber) {
                        // 确保订阅房间主题并重新获取房间状态
                        if (!roomSubscription || !roomSubscription.active) {
                            joinAndSubscribe({ roomNumber: roomNumber });
                        }
                        // 重连后发送重连请求以获取最新状态
                        stompClient.send("/app/rejoin-with-id", {}, JSON.stringify(buildRejoinPayload(roomNumber, storedPlayerId)));
                        
                        // 等待房间状态更新后，再尝试点击上车按钮
                        // 使用 setTimeout 等待状态更新
                        setTimeout(() => {
                            if (stompClient && stompClient.connected && currentRoom && currentRoom.roomNumber) {
                                // 递归调用，此时连接应该已建立
                                btnStartMission.onclick();
                            } else {
                                hideLoader();
                                showError('identity-error', '重连失败，请刷新页面重试');
                            }
                        }, 500);
                    } else {
                        hideLoader();
                        showError('identity-error', '无法获取房间信息');
                    }
                }, true);
                return;
            } else {
                console.error("WebSocket 连接未建立且无法重连");
                showError('identity-error', '连接已断开，请刷新页面重试');
                return;
            }
        }
        
        if (!currentRoom || !currentRoom.roomNumber) {
            console.error("当前房间信息不存在");
            showError('identity-error', '房间信息不存在，请返回房间页面');
            return;
        }
        
        console.log("发送开始任务消息，房间号:", currentRoom.roomNumber);
        hideError('identity-error');
        showLoader("开始任务中...");
        
        try {
            // 检查当前任务状态
            const round = currentRoom.currentMissionRound || 0;
            const selectedMembers = currentRoom.selectedMissionMembers || [];
            const membersArray = Array.isArray(selectedMembers) ? selectedMembers : Array.from(selectedMembers);
            const votingCompleted = currentRoom.missionVotingCompleted || false;
            const missionResult = currentRoom.missionResult;
            
            // 如果上一轮任务已完成，允许开始新任务（不检查人数）
            const isPreviousRoundCompleted = votingCompleted && missionResult != null;
            
            // 如果当前轮次任务未完成且人数已满，不跳转页面，只显示提示
            if (round > 0 && !isPreviousRoundCompleted && membersArray.length > 0) {
                const requiredSize = getRequiredMissionSize(currentRoom.totalPlayers, round);
                if (membersArray.length >= requiredSize) {
                    hideLoader();
                    showError('identity-error', `任务人数已满，需要 ${requiredSize} 人`);
                    return;
                }
            }
            
            // 先切换到投票页面（只有点击的玩家）
            // 后端会将玩家加入任务，然后前端会收到状态更新并刷新投票页面
            showVotingPage();
            
            stompClient.send("/app/start-mission", {}, JSON.stringify({
                roomNumber: currentRoom.roomNumber
            }));
            console.log("开始任务消息已发送");
        } catch (e) {
            console.error("发送消息失败:", e);
            hideLoader();
            showError('identity-error', '发送消息失败: ' + e.message);
        }
    };
}

// 移除确认按钮逻辑，因为现在是玩家自己选择是否上车

if (btnCancelMission) {
    btnCancelMission.onclick = () => {
        showPage('identity');
    };
}

if (btnVoteSuccess) {
    btnVoteSuccess.onclick = () => {
        if (!stompClient || !stompClient.connected || !currentRoom) {
            showError('voting-error', '连接已断开');
            return;
        }
        btnVoteSuccess.disabled = true;
        btnVoteFail.disabled = true;
        showLoader("投票中...");
        stompClient.send("/app/vote", {}, JSON.stringify({
            roomNumber: currentRoom.roomNumber,
            vote: 'success'
        }));
    };
}

if (btnVoteFail) {
    btnVoteFail.onclick = () => {
        if (!stompClient || !stompClient.connected || !currentRoom) {
            showError('voting-error', '连接已断开');
            return;
        }
        
        // 检查身份，好人阵营（除了兰斯洛特（蓝））不能投失败
        let myIdentity = null;
        let myCamp = null;
        try {
            const storedIdentityData = localStorage.getItem('avalonIdentityData');
            if (storedIdentityData) {
                const identityData = JSON.parse(storedIdentityData);
                myIdentity = identityData.identity;
                myCamp = identityData.camp;
            }
        } catch (e) {
            console.error("读取身份信息失败:", e);
        }
        
        const canVoteFail = myCamp === 'evil' || myIdentity === '兰斯洛特（蓝）';
        if (!canVoteFail) {
            showError('voting-error', '好人阵营只能投任务成功！');
            return;
        }
        
        btnVoteSuccess.disabled = true;
        btnVoteFail.disabled = true;
        showLoader("投票中...");
        stompClient.send("/app/vote", {}, JSON.stringify({
            roomNumber: currentRoom.roomNumber,
            vote: 'fail'
        }));
    };
}

if (btnBackToIdentity) {
    btnBackToIdentity.onclick = () => {
        showPage('identity');
    };
}

if (btnBackToIdentityFromVoting) {
    btnBackToIdentityFromVoting.onclick = () => {
        // 退出任务（释放坑位）
        if (stompClient && stompClient.connected && currentRoom) {
            try {
                stompClient.send("/app/leave-mission", {}, JSON.stringify({
                    roomNumber: currentRoom.roomNumber
                }));
                console.log("退出任务消息已发送");
            } catch (e) {
                console.error("发送退出任务消息失败:", e);
            }
        }
        showPage('identity');
    };
}

if (sliderTotalPlayers && sliderValueDisplay) {
    sliderTotalPlayers.oninput = (e) => {
        sliderValueDisplay.textContent = e.target.value;
    };
    sliderValueDisplay.textContent = sliderTotalPlayers.value;
}

if (btnCreateRoom) {
    btnCreateRoom.onclick = () => {
        const r = inputRoomNumber.value;
        const t = parseInt(sliderTotalPlayers.value);
        if (!/^\d{4,6}$/.test(r)) {
            showError('home-error', '请输入 4-6 位数字房间号');
            return;
        }
        hideError('home-error');
        showLoader("创建房间中...");
        connect(() => {
            // 延迟检查，确保连接和订阅都已完全建立
            setTimeout(() => {
                if (!stompClient || !stompClient.connected) {
                    console.error("🔄 [创建房间] ❌ 连接未建立，无法创建房间");
                    hideLoader();
                    showError('home-error', '连接未建立，请重试');
                    return;
                }
                if (!mySessionId) {
                    console.error("🔄 [创建房间] ❌ sessionId 未设置，无法创建房间");
                    hideLoader();
                    showError('home-error', '连接信息不完整，请刷新页面重试');
                    return;
                }
                if (!messageSubscriptions.roomCreated) {
                    console.error("🔄 [创建房间] ❌ room-created 订阅不存在，无法创建房间");
                    hideLoader();
                    showError('home-error', '订阅未建立，请刷新页面重试');
                    return;
                }
                
                // 再次检查 sessionId 是否匹配（防止在连接过程中 sessionId 发生变化）
                const currentSessionId = mySessionId;
                const payload = buildCreatePayload(r, t);
                console.log("🔄 [创建房间] 发送创建房间请求: roomNumber=" + r + ", sessionId=" + currentSessionId + ", subscription=" + (messageSubscriptions.roomCreated ? 'exists' : 'null'));
                
                // 清除之前的超时定时器
                if (createRoomTimeout) {
                    clearTimeout(createRoomTimeout);
                    createRoomTimeout = null;
                }
                
                // 设置超时检查：如果5秒内没有收到 room-created 消息，提示用户
                createRoomTimeout = setTimeout(() => {
                    if (!currentRoom || currentRoom.roomNumber !== r) {
                        console.warn("🔄 [创建房间] ⚠️ 5秒内未收到 room-created 响应，可能连接已断开或 sessionId 不匹配");
                        console.warn("🔄 [创建房间] 当前 sessionId=" + mySessionId + ", 发送时 sessionId=" + currentSessionId);
                        hideLoader();
                        showError('home-error', '创建房间超时，请检查网络连接后重试');
                        createRoomTimeout = null;
                    }
                }, 5000);
                
                stompClient.send("/app/create-room", {}, JSON.stringify(payload));
            }, 800); // 延迟 800ms 确保连接和订阅都已完全建立
        });
    };
}

if (btnJoinRoom) {
    btnJoinRoom.onclick = () => {
        const r = inputRoomNumber.value;
        if (!/^\d{4,6}$/.test(r)) {
            showError('home-error', '请输入 4-6 位数字房间号');
            return;
        }
        hideError('home-error');
        showLoader("加入房间中...");
        connect(() => {
            if (!stompClient || !stompClient.connected) {
                hideLoader();
                return;
            }
            currentRoom = { roomNumber: r };
            localStorage.setItem('avalonRoomNumber', r);
            joinAndSubscribe({ roomNumber: r });
            
            // 如果有 playerId，尝试使用 rejoin-with-id 恢复座位号
            const storedPlayerId = localStorage.getItem('avalonPlayerId');
            if (storedPlayerId) {
                console.log("🔄 [加入房间] 检测到 playerId，尝试使用 rejoin-with-id 恢复座位号: playerId=" + storedPlayerId);
                setTimeout(() => {
                    if (stompClient && stompClient.connected) {
                        stompClient.send("/app/rejoin-with-id", {}, JSON.stringify(buildRejoinPayload(r, storedPlayerId)));
                    }
                }, 300);
            } else {
                // 没有 playerId，使用普通的 join-room
                stompClient.send("/app/join-room", {}, JSON.stringify(buildJoinPayload(r)));
            }
        });
    };
}

function joinAndSubscribe(roomData) {
    if (!stompClient || !stompClient.connected) {
        showError('home-error', "连接已断开");
        disconnect(true);
        hideLoader();
        return;
    }

    const rts = roomData.roomNumber;
    if (typeof rts !== 'string' || !/^\d+$/.test(rts)) {
        console.error("无效的房间号:", roomData);
        showError('room-error', "房间号无效");
        disconnect(true);
        hideLoader();
        return;
    }

    if (!currentRoom || !currentRoom.roomNumber) {
        currentRoom = { roomNumber: rts };
    } else {
        currentRoom.roomNumber = rts;
    }
    if (roomInfoNumber) roomInfoNumber.textContent = rts;

    if (roomSubscription) {
        try {
            roomSubscription.unsubscribe();
        } catch (e) {}
    }

    try {
        roomSubscription = stompClient.subscribe(`/topic/room/${rts}`, onRoomStateUpdate);
    } catch (e) {
        console.error("订阅失败:", e);
        showError('room-error', "订阅失败");
        disconnect(true);
        hideLoader();
        return;
    }
    showPage('room');
}

function selectSeat(seatNumber) {
    if (!stompClient || !stompClient.connected || !currentRoom) {
        console.error("无法选座");
        return;
    }
    showLoader("选座中...");
    stompClient.send("/app/select-seat", {}, JSON.stringify({
        roomNumber: currentRoom.roomNumber,
        seatNumber: seatNumber
    }));
}

function requestReadyState(targetReadyState = null, options = {}) {
    if (!stompClient || !stompClient.connected || !currentRoom || !mySessionId) {
        console.error("无法发送准备状态");
        return;
    }
    const desiredState = typeof targetReadyState === 'boolean' ? targetReadyState : !lastReadyState;
    const silent = options.silent === true;
    if (!silent && btnReady) {
        btnReady.disabled = true;
        showLoader(desiredState ? "准备中..." : "取消中...");
    }
    stompClient.send("/app/ready", {}, JSON.stringify({
        roomNumber: currentRoom.roomNumber,
        isReady: desiredState
    }));
}

if (btnReady) {
    btnReady.onclick = () => {
        requestReadyState();
    };
}

if (btnLeaveRoom) {
    btnLeaveRoom.onclick = () => {
        // 退出房间时，保留 playerId 以便重新进入时可以使用 rejoin-with-id 恢复座位号
        // 只清空房间号和游戏相关数据，不清空 playerId
        const savedPlayerId = localStorage.getItem('avalonPlayerId');
        disconnect(true);
        // 恢复 playerId，这样重新进入房间时可以使用 rejoin-with-id
        if (savedPlayerId) {
            localStorage.setItem('avalonPlayerId', savedPlayerId);
            myPlayerId = savedPlayerId;
            console.log("🔄 [退出房间] 保留 playerId 以便重新进入时恢复座位号: " + savedPlayerId);
        }
        updateReconnectButton();
    };
}

if (btnReturnToRoom) {
    btnReturnToRoom.onclick = () => {
        // 添加确认对话框
        if (!confirm('确定要返回房间开始下一局吗？当前游戏进度将丢失。')) {
            return; // 取消操作，直接返回
        }
        
        try {
            localStorage.removeItem('avalonIdentityData');
            localStorage.removeItem('avalonGameInProgress');
        } catch (e) {
            console.error("清除身份失败:", e);
        }

        if (!stompClient || !stompClient.connected) {
            const storedRoomNumber = localStorage.getItem('avalonRoomNumber');
            const storedPlayerId = localStorage.getItem('avalonPlayerId');
            if (storedRoomNumber && storedPlayerId) {
                showLoader("连接中...");
                connect(() => {
                    const roomNumber = (currentRoom && currentRoom.roomNumber) || storedRoomNumber;
                    if (roomNumber) {
                        // 确保订阅房间主题
                        if (!roomSubscription || !roomSubscription.active) {
                            joinAndSubscribe({ roomNumber: roomNumber });
                        }
                        stompClient.send("/app/return-to-room", {}, JSON.stringify({ roomNumber: roomNumber }));
                    }
                }, false);
            } else {
                showError('home-error', '无法返回房间：未找到房间信息');
                disconnect(true);
            }
        } else {
            const roomNumber = (currentRoom && currentRoom.roomNumber) || localStorage.getItem('avalonRoomNumber');
            if (roomNumber) {
                showLoader("返回房间中...");
                // 确保订阅房间主题（即使已订阅，重新订阅也没问题）
                if (!roomSubscription || !roomSubscription.active) {
                    joinAndSubscribe({ roomNumber: roomNumber });
                }
                stompClient.send("/app/return-to-room", {}, JSON.stringify({ roomNumber: roomNumber }));
            } else {
                showError('home-error', '无法返回房间：未找到房间号');
            }
        }
    };
}

function kickPlayer(event, targetPlayerId, seatNumber) {
    event.stopPropagation();
    if (!stompClient || !stompClient.connected || !currentRoom) return;
    if (confirm(`确定要将 ${seatNumber} 号玩家移出房间吗？`)) {
        showLoader("踢人中...");
        stompClient.send("/app/kick-player", {}, JSON.stringify({
            roomNumber: currentRoom.roomNumber,
            targetPlayerId: targetPlayerId
        }));
    }
}

function transferHost(event, targetPlayerId, seatNumber) {
    event.stopPropagation();
    if (!stompClient || !stompClient.connected || !currentRoom) return;
    if (confirm(`确定要将房主权限转让给 ${seatNumber} 号玩家吗？`)) {
        showLoader("转让中...");
        stompClient.send("/app/transfer-host", {}, JSON.stringify({
            roomNumber: currentRoom.roomNumber,
            targetPlayerId: targetPlayerId
        }));
    }
}

function showPage(pageId) {
    if (!pages || !pages[pageId]) {
        console.error("无效的页面 ID:", pageId);
        return;
    }
    Object.values(pages).forEach(page => {
        if (page) page.classList.remove('active');
    });
    pages[pageId].classList.add('active');
    hideError('home-error');
    hideError('room-error');
    hideError('identity-error');
    hideError('mission-error');
    hideError('voting-error');
    
    // 如果切换到房间页面，显示配置说明
    if (pageId === 'room') {
        // 延迟一点调用，确保 DOM 已更新
        setTimeout(() => {
            if (currentRoom) {
                updatePlayerConfigInfo(currentRoom);
            }
        }, 10);
    }
    
    try {
        localStorage.setItem('avalonLastPage', pageId);
    } catch (e) {}
    
    // 页面切换后，更新重连按钮状态（确保重连按钮在正确的页面显示）
    setTimeout(() => {
        updateReconnectButton();
    }, 50);
}

function showError(errorElementId, message) {
    const errorDiv = document.getElementById(errorElementId);
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    } else {
        console.error(`错误元素未找到: ${errorElementId}, 消息: ${message}`);
    }
}

function hideError(errorElementId) {
    const errorDiv = document.getElementById(errorElementId);
    if (errorDiv) {
        errorDiv.style.display = 'none';
    }
}

function showLoader(message = "加载中...") {
    if (loaderOverlay && loaderText) {
        loaderText.textContent = message;
        loaderOverlay.classList.add('active');
    }
}

function hideLoader() {
    if (loaderOverlay) {
        loaderOverlay.classList.remove('active');
    }
}

function tryAutoReconnect(reason) {
    console.log("🔄 [自动重连] 尝试自动重连，原因:", reason);
    // 先更新按钮状态
    updateReconnectButton();
    
    if (stompClient && stompClient.connected) {
        console.log("🔄 [自动重连] ✅ 已连接，无需重连");
        updateReconnectButton();
        return;
    }
    const storedRoomNumber = localStorage.getItem('avalonRoomNumber');
    const storedPlayerId = localStorage.getItem('avalonPlayerId');
    if (!storedRoomNumber || !storedPlayerId) {
        console.log("🔄 [自动重连] ❌ 缺少房间或玩家信息，无法自动重连: roomNumber=" + storedRoomNumber + ", playerId=" + storedPlayerId);
        updateReconnectButton();
        return;
    }
    console.log("🔄 [自动重连] 开始重连，房间号:", storedRoomNumber, "玩家ID:", storedPlayerId);
    showLoader("恢复连接中...");
    connect(() => {
        // 重连后需要等待订阅建立，然后发送重连请求
        if (storedRoomNumber && storedPlayerId) {
            // 确保订阅房间主题
            if (!roomSubscription || !roomSubscription.active) {
                console.log("🔄 [自动重连] 订阅房间主题: roomNumber=" + storedRoomNumber);
                joinAndSubscribe({ roomNumber: storedRoomNumber });
            }
            
            // 延迟发送重连请求，确保订阅已建立
            setTimeout(() => {
                if (stompClient && stompClient.connected) {
                    console.log("🔄 [自动重连] 发送重连请求: roomNumber=" + storedRoomNumber + ", playerId=" + storedPlayerId);
                            stompClient.send("/app/rejoin-with-id", {}, JSON.stringify(buildRejoinPayload(storedRoomNumber, storedPlayerId)));
                } else {
                    console.error("🔄 [自动重连] ❌ 连接未建立，无法发送重连请求");
                }
            }, 300);
        }
        
        setTimeout(() => {
            hideLoader();
            updateReconnectButton();
            console.log("🔄 [自动重连] ✅ 重连完成");
        }, 1500);
    }, true);
    
    // 如果连接失败，也要清除标志
    setTimeout(() => {
        if (!stompClient || !stompClient.connected) {
            isAutoReconnecting = false;
        }
    }, 5000);
}

// 页面可见性变化监听（支持标准浏览器和微信）
let visibilityChangeHandled = false;
document.addEventListener('visibilitychange', () => {
    console.log("🔄 [页面事件] visibilitychange 触发, visibilityState:", document.visibilityState);
    if (document.visibilityState === 'visible') {
        visibilityChangeHandled = true;
        // 页面从后台恢复时，先更新按钮状态
        updateReconnectButton();
        
        // 检查是否有房间信息，如果有，确保不在首页
        const storedRoomNumber = localStorage.getItem('avalonRoomNumber');
        const storedPlayerId = localStorage.getItem('avalonPlayerId');
        if (storedRoomNumber && storedPlayerId) {
            // 如果有房间信息，确保不在首页（可能在身份页面或房间页面）
            if (pages.home && pages.home.classList.contains('active')) {
                const lastPage = localStorage.getItem('avalonLastPage');
                const gameInProgress = localStorage.getItem('avalonGameInProgress') === 'true';
                const hasIdentityData = localStorage.getItem('avalonIdentityData') != null;
                
                if (gameInProgress || hasIdentityData || lastPage === 'identity' || lastPage === 'mission' || lastPage === 'voting' || lastPage === 'missionResult') {
                    // 如果之前在游戏页面，显示身份页面
                    showPage('identity');
                    console.log("🔄 [页面事件] 从后台恢复，有房间信息，跳转到身份页面");
                } else if (lastPage === 'room') {
                    showPage('room');
                    console.log("🔄 [页面事件] 从后台恢复，有房间信息，跳转到房间页面");
                }
            }
        }
        
        // 延迟一点再尝试自动重连，给微信时间恢复页面
        setTimeout(() => {
            tryAutoReconnect('前台恢复(visibilitychange)');
            // 重置标志，允许后续的 focus 事件处理
            setTimeout(() => {
                visibilityChangeHandled = false;
            }, 1000);
        }, 500);
    } else {
        // 页面进入后台时，也更新一次按钮状态
        updateReconnectButton();
        visibilityChangeHandled = false;
    }
});

// 页面显示事件监听（微信常用，特别是从后台恢复时）
window.addEventListener('pageshow', (event) => {
    console.log("🔄 [页面事件] pageshow 触发, persisted:", event.persisted);
    // persisted 为 true 表示页面是从缓存中恢复的
    if (event.persisted) {
        console.log("🔄 [页面事件] 页面从缓存恢复，检查连接状态");
    }
    
    // 检查是否有房间信息，如果有，确保不在首页
    const storedRoomNumber = localStorage.getItem('avalonRoomNumber');
    const storedPlayerId = localStorage.getItem('avalonPlayerId');
    if (storedRoomNumber && storedPlayerId) {
        // 如果有房间信息，确保不在首页（可能在身份页面或房间页面）
        if (pages.home && pages.home.classList.contains('active')) {
            const lastPage = localStorage.getItem('avalonLastPage');
            const gameInProgress = localStorage.getItem('avalonGameInProgress') === 'true';
            const hasIdentityData = localStorage.getItem('avalonIdentityData') != null;
            
            if (gameInProgress || hasIdentityData || lastPage === 'identity' || lastPage === 'mission' || lastPage === 'voting' || lastPage === 'missionResult') {
                // 如果之前在游戏页面，显示身份页面
                showPage('identity');
                console.log("🔄 [页面事件] 页面恢复，有房间信息，跳转到身份页面");
            } else if (lastPage === 'room') {
                showPage('room');
                console.log("🔄 [页面事件] 页面恢复，有房间信息，跳转到房间页面");
            }
        }
    }
    
    // 延迟更新，确保页面已完全恢复
    setTimeout(() => {
        updateReconnectButton();
        // 如果页面是从缓存恢复的，需要检查连接状态
        if (event.persisted || !(stompClient && stompClient.connected)) {
            setTimeout(() => {
                tryAutoReconnect('页面恢复(pageshow)');
            }, 1000);
        }
    }, 300);
});

// 窗口获得焦点事件监听（微信中也可能触发）
let focusHandled = false;
window.addEventListener('focus', () => {
    console.log("🔄 [页面事件] focus 触发");
    
    // 防止与 visibilitychange 重复处理
    if (visibilityChangeHandled) {
        console.log("🔄 [页面事件] visibilitychange 已处理，忽略 focus 事件");
        return;
    }
    
    // 防止重复处理
    if (focusHandled) {
        console.log("🔄 [页面事件] focus 已处理，忽略重复触发");
        return;
    }
    focusHandled = true;
    
    setTimeout(() => {
        updateReconnectButton();
        // 延迟检查连接状态
        setTimeout(() => {
            if (!(stompClient && stompClient.connected)) {
                tryAutoReconnect('窗口获得焦点(focus)');
            }
            // 重置标志
            setTimeout(() => {
                focusHandled = false;
            }, 1000);
        }, 500);
    }, 200);
});

// 窗口失去焦点事件监听
window.addEventListener('blur', () => {
    console.log("🔄 [页面事件] blur 触发");
    updateReconnectButton();
});

// 页面隐藏事件监听
window.addEventListener('pagehide', (event) => {
    console.log("🔄 [页面事件] pagehide 触发, persisted:", event.persisted);
    updateReconnectButton();
});

window.addEventListener('online', () => {
    console.log("🔄 [网络事件] online 触发");
    updateReconnectButton();
    tryAutoReconnect('网络恢复');
});

window.addEventListener('offline', () => {
    console.log("🔄 [网络事件] offline 触发");
    updateReconnectButton();
});

// 定期检查连接状态（每3秒检查一次）
setInterval(() => {
    const hasRoom = localStorage.getItem('avalonRoomNumber');
    const hasPlayerId = localStorage.getItem('avalonPlayerId');
    const isConnected = stompClient && stompClient.connected;
    
    // 如果有房间信息但未连接，显示重连按钮
    if (hasRoom && hasPlayerId && !isConnected) {
        updateReconnectButton();
    }
}, 3000);

// 微信环境特殊处理：监听 WeixinJSBridgeReady（如果可用）
if (typeof WeixinJSBridge !== 'undefined') {
    console.log("🔄 [微信环境] WeixinJSBridge 已可用");
} else {
    document.addEventListener('WeixinJSBridgeReady', () => {
        console.log("🔄 [微信环境] WeixinJSBridgeReady 触发");
        setTimeout(() => {
            updateReconnectButton();
            if (!(stompClient && stompClient.connected)) {
                tryAutoReconnect('微信环境就绪');
            }
        }, 500);
    });
}

function testDisconnectReconnect() {
    if (!stompClient || !stompClient.connected) {
        console.warn("当前未连接，无法测试重连");
        return;
    }

    const storedRoomNumber = localStorage.getItem('avalonRoomNumber');
    const storedPlayerId = localStorage.getItem('avalonPlayerId');

    if (!storedRoomNumber || !storedPlayerId) {
        console.warn("缺少房间或玩家信息，无法重连");
        return;
    }

    showLoader("测试断线重连中...");
    disconnect(false);

    setTimeout(() => {
        connect(() => {
            hideLoader();
            if (storedRoomNumber) {
                joinAndSubscribe({ roomNumber: storedRoomNumber });
                if (storedPlayerId) {
                    stompClient.send("/app/rejoin-with-id", {}, JSON.stringify(buildRejoinPayload(storedRoomNumber, storedPlayerId)));
                }
            }
        }, true);
    }, 1000);
}

// 手动重新连接
function manualReconnect() {
    const storedRoomNumber = localStorage.getItem('avalonRoomNumber');
    const storedPlayerId = localStorage.getItem('avalonPlayerId');

    if (!storedRoomNumber || !storedPlayerId) {
        console.error("🔄 [手动重连] ❌ 缺少房间或玩家信息: roomNumber=" + storedRoomNumber + ", playerId=" + storedPlayerId);
        showError('home-error', '缺少房间或玩家信息，无法重连');
        return;
    }

    console.log("🔄 [手动重连] 开始手动重连: roomNumber=" + storedRoomNumber + ", playerId=" + storedPlayerId);
    showLoader("重新连接中...");
    
    // 重新连接
    connect(() => {
        if (storedRoomNumber) {
            // 确保订阅房间主题
            if (!roomSubscription || !roomSubscription.active) {
                console.log("🔄 [手动重连] 订阅房间主题: roomNumber=" + storedRoomNumber);
                joinAndSubscribe({ roomNumber: storedRoomNumber });
            }
            
            // 延迟发送重连请求，确保订阅已建立（增加延迟时间）
            setTimeout(() => {
                if (stompClient && stompClient.connected && storedPlayerId) {
                    console.log("🔄 [手动重连] 发送重连请求: roomNumber=" + storedRoomNumber + ", playerId=" + storedPlayerId);
                    stompClient.send("/app/rejoin-with-id", {}, JSON.stringify(buildRejoinPayload(storedRoomNumber, storedPlayerId)));
                    // 等待状态更新
                    setTimeout(() => {
                        hideLoader();
                        updateReconnectButton();
                        console.log("🔄 [手动重连] ✅ 重连完成");
                    }, 1000);
                } else {
                    console.error("🔄 [手动重连] ❌ 连接未建立: connected=" + (stompClient ? stompClient.connected : false));
                    hideLoader();
                    showError('identity-error', '重连失败：连接未建立');
                    updateReconnectButton();
                }
            }, 300); // 增加延迟到 300ms
        } else {
            console.error("🔄 [手动重连] ❌ 缺少房间号");
            hideLoader();
            showError('identity-error', '重连失败：缺少房间号');
            updateReconnectButton();
        }
    }, true);
}

// 更新重连按钮显示状态
function updateReconnectButton() {
    const btnReconnect = document.getElementById('btn-reconnect');
    const reconnectOverlay = document.getElementById('reconnect-overlay');
    
    // 如果元素还不存在，稍后再试（可能在DOM加载前调用）
    if (!btnReconnect || !reconnectOverlay) {
        console.log("🔄 [重连按钮] 元素未就绪，稍后重试");
        // 如果 DOM 已加载但元素不存在，延迟重试
        if (document.readyState === 'complete' || document.readyState === 'interactive') {
            setTimeout(() => {
                updateReconnectButton();
            }, 500);
        }
        return;
    }
    
    const isConnected = stompClient && stompClient.connected;
    const hasRoom = localStorage.getItem('avalonRoomNumber');
    const hasPlayerId = localStorage.getItem('avalonPlayerId');
    
    console.log("🔄 [重连按钮] 更新状态 - 连接:", isConnected, "房间:", hasRoom, "玩家ID:", hasPlayerId);
    
    // 如果有房间信息和玩家ID但未连接，显示遮盖层和按钮
    if (hasRoom && hasPlayerId && !isConnected) {
        reconnectOverlay.style.display = 'flex';
        btnReconnect.disabled = false;
        // 禁用页面所有交互
        document.body.style.pointerEvents = 'none';
        reconnectOverlay.style.pointerEvents = 'auto';
        console.log("🔄 [重连按钮] ✅ 显示重连遮盖层: 有房间信息但未连接");
    } else {
        reconnectOverlay.style.display = 'none';
        // 恢复页面交互
        document.body.style.pointerEvents = 'auto';
        if (isConnected) {
            console.log("🔄 [重连按钮] ❌ 隐藏重连遮盖层: 已连接");
        } else if (!hasRoom) {
            console.log("🔄 [重连按钮] ❌ 隐藏重连遮盖层: 无房间信息");
        } else if (!hasPlayerId) {
            console.log("🔄 [重连按钮] ❌ 隐藏重连遮盖层: 无玩家ID");
        }
    }
}

// 确保消息监听器在脚本加载时立即注册
(function() {
    // 防止重复注册
    if (window.__messageListenerRegistered) {
        return;
    }
    window.__messageListenerRegistered = true;
    
    window.addEventListener('message', (event) => {
        // 检查 origin 是否匹配（允许空字符串、'null' 或当前 origin）
        var originMatches = event.origin === window.location.origin || 
                           event.origin === '' || 
                           event.origin === 'null' ||
                           event.origin === window.location.protocol + '//' + window.location.host;
        
        if (!originMatches) {
            return;
        }
        
        // 处理 PING 消息（用于测试消息通道）
        if (event.data && event.data.type === 'PING') {
            try {
                if (event.source && event.source !== window) {
                    event.source.postMessage({
                        type: 'PONG'
                    }, event.origin);
                }
            } catch (e) {
                // 忽略错误
            }
        }
        
        // 发送确认消息给父窗口（如果是测试页面发送的）
        if (event.source && event.source !== window && event.data && event.data.type) {
            try {
                event.source.postMessage({
                    type: 'TEST_MESSAGE_RECEIVED',
                    originalType: event.data.type
                }, event.origin);
            } catch (e) {
                // 忽略错误
            }
        }
        
        if (event.data && event.data.type === 'TEST_DISCONNECT_RECONNECT') {
            testDisconnectReconnect();
        }
        if (event.data && event.data.type === 'TEST_READY_TOGGLE') {
            requestReadyState(null, { silent: true });
        }
        if (event.data && event.data.type === 'TEST_READY_SET') {
            requestReadyState(!!event.data.readyState, { silent: true });
        }
    });
})();

// 重连按钮事件监听（在DOMContentLoaded之前注册，因为按钮在HTML中）
(function() {
    const btnReconnectElement = document.getElementById('btn-reconnect');
    if (btnReconnectElement) {
        btnReconnectElement.onclick = () => {
            if (btnReconnectElement.disabled) return;
            btnReconnectElement.disabled = true;
            manualReconnect();
        };
    }
})();

// 页面加载完成后立即检查连接状态（支持微信，提前执行）
(function() {
    function initReconnectCheck() {
        console.log("🔄 [初始化] 页面加载完成，检查连接状态");
        updateReconnectButton();
        
        // 延迟再次检查，确保所有初始化都完成（特别是微信环境）
        setTimeout(() => {
            updateReconnectButton();
        }, 1000);
    }
    
    // DOMContentLoaded 时检查
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initReconnectCheck);
    } else {
        // DOM 已经加载完成，立即检查
        initReconnectCheck();
    }
    
    // window.onload 时也检查一次（确保所有资源加载完成，微信中很重要）
    window.addEventListener('load', () => {
        console.log("🔄 [初始化] window.onload 触发，检查连接状态");
        setTimeout(() => {
            updateReconnectButton();
        }, 500);
    });
})();

document.addEventListener('DOMContentLoaded', () => {
    // 注意：不要在这里调用 disconnectCleanup()，因为这会清空 currentRoom
    // 如果页面是从缓存恢复的，我们需要保留房间信息以便重连
    // disconnectCleanup();
    
    // 只清理订阅，不清空房间信息
    if (roomSubscription) {
        try {
            roomSubscription.unsubscribe();
        } catch (e) {}
        roomSubscription = null;
    }

    const storedIdentityDataString = localStorage.getItem('avalonIdentityData');
    const gameInProgress = localStorage.getItem('avalonGameInProgress') === 'true';
    const storedRoomValue = localStorage.getItem('avalonRoomNumber');
    const storedPlayerId = localStorage.getItem('avalonPlayerId');
    myPlayerId = storedPlayerId;
    
    console.log("🔄 [页面加载] 检测到状态: gameInProgress=" + gameInProgress + 
               ", hasIdentityData=" + (storedIdentityDataString != null) + 
               ", roomNumber=" + storedRoomValue + ", playerId=" + storedPlayerId);
    
    // 初始化重连按钮状态（多次检查确保微信中也能正确显示）
    // 延迟一下，确保 DOM 已完全加载
    setTimeout(() => {
        updateReconnectButton();
    }, 100);
    
    // 延迟检查，给微信时间初始化页面和连接状态
    setTimeout(() => {
        updateReconnectButton();
    }, 500);
    
    // 再次延迟检查，确保重连按钮正确显示
    setTimeout(() => {
        updateReconnectButton();
    }, 2000);

    let actualRoomNumber = null;
    if (storedRoomValue) {
        try {
            const parsedRoom = JSON.parse(storedRoomValue);
            if (parsedRoom && typeof parsedRoom.roomNumber === 'string') {
                actualRoomNumber = parsedRoom.roomNumber;
                localStorage.setItem('avalonRoomNumber', actualRoomNumber);
            } else if (typeof storedRoomValue === 'string') {
                actualRoomNumber = storedRoomValue;
            }
        } catch (e) {
            actualRoomNumber = storedRoomValue;
        }
    }

    const lastPage = localStorage.getItem('avalonLastPage');

    if (gameInProgress && storedIdentityDataString && identityCard) {
        try {
            const identityData = JSON.parse(storedIdentityDataString);
            identityCard.className = `card ${identityData.camp}`;
            identityImage.src = identityData.imageUrl;
            identityName.textContent = identityData.identity;
            seenTitle.textContent = identityData.seenTitle;
            seenPlayers.textContent = identityData.seenPlayers.length > 0 ? identityData.seenPlayers.join(', ') : '—';
            seenSubtitle.textContent = identityData.seenSubtitle;
            leaderSeatNumber.textContent = identityData.leaderSeat;
            if (actualRoomNumber) currentRoom = { roomNumber: actualRoomNumber };
            showPage('identity');
            hideLoader();
            if (actualRoomNumber && storedPlayerId) {
                // 先更新按钮状态（显示重连按钮，因为还没有连接）
                updateReconnectButton();
                showLoader("后台恢复连接中...");
                connect(() => {
                    // 等待连接建立并收到房间状态更新后再隐藏加载器
                    // onRoomStateUpdate 会自动更新任务历史
                    setTimeout(() => {
                        hideLoader();
                        updateReconnectButton();
                    }, 1000);
                }, true);
            } else {
                // 如果没有房间信息，更新按钮状态（应该隐藏）
                updateReconnectButton();
            }
        } catch (e) {
            console.error("解析本地身份失败:", e);
            localStorage.removeItem('avalonIdentityData');
            localStorage.removeItem('avalonGameInProgress');
            showPage('home');
            hideLoader();
        }
    } else if (actualRoomNumber && storedPlayerId) {
        // 有房间号和玩家ID，但没有身份数据（可能是长时间断线后身份数据丢失）
        // 先尝试恢复身份数据，如果后端重连成功，会重新发送身份信息
        console.log("🔄 [页面加载] 检测到房间信息但无身份数据，尝试恢复: roomNumber=" + actualRoomNumber + ", lastPage=" + lastPage);
        
        // 不要删除身份数据，可能只是暂时丢失
        // localStorage.removeItem('avalonIdentityData');
        // localStorage.removeItem('avalonGameInProgress');
        
        localStorage.setItem('avalonRoomNumber', actualRoomNumber);
        
        // 恢复 currentRoom，以便重连后能正确识别
        currentRoom = { roomNumber: actualRoomNumber };
        
        // 先更新按钮状态（显示重连按钮），确保重连按钮显示
        // 延迟一下，确保 DOM 已完全加载
        setTimeout(() => {
            updateReconnectButton();
        }, 100);
        
        // 根据 lastPage 决定显示哪个页面，但优先显示身份页面（如果游戏已开始）
        // 如果 lastPage 是游戏相关页面，说明之前可能在游戏中，应该显示身份页面等待重连
        if (lastPage === 'identity' || lastPage === 'mission' || lastPage === 'voting' || lastPage === 'missionResult') {
            // 如果之前在游戏页面，先显示身份页面（等待重连后确认游戏状态）
            showPage('identity');
            console.log("🔄 [页面加载] 之前在游戏页面，显示身份页面等待重连");
        } else if (lastPage === 'room') {
            showPage('room');
            console.log("🔄 [页面加载] 之前在房间页面，显示房间页面等待重连");
        } else {
            // 如果 lastPage 不存在或是 home，但有房间信息，说明可能是长时间断线
            // 这种情况下，不跳转到首页，而是显示身份页面等待重连
            // 因为如果有房间信息，说明之前肯定在游戏中
            showPage('identity');
            console.log("🔄 [页面加载] 有房间信息但 lastPage 不存在，显示身份页面等待重连");
        }
        
        // 再次更新按钮状态，确保重连按钮显示
        setTimeout(() => {
            updateReconnectButton();
        }, 300);
        
        showLoader("恢复房间中...");
        connect(() => {
            // 重连后，onRoomStateUpdate 会根据游戏状态自动跳转到正确的页面
            setTimeout(() => {
                hideLoader();
                updateReconnectButton();
            }, 2000);
        }, true);
    } else {
        // 完全没有房间信息，清理所有数据并显示首页
        console.log("🔄 [页面加载] 没有房间信息，显示首页");
        localStorage.removeItem('avalonRoomNumber');
        localStorage.removeItem('avalonPlayerId');
        localStorage.removeItem('avalonIdentityData');
        localStorage.removeItem('avalonGameInProgress');
        showPage('home');
        hideLoader();
        // 确保重连按钮隐藏
        setTimeout(() => {
            updateReconnectButton();
        }, 100);
    }

    if (sliderTotalPlayers && sliderValueDisplay) {
        sliderValueDisplay.textContent = sliderTotalPlayers.value;
    }
    
    // 初始化调试控制台
    initDebugConsole();
});

// 调试控制台功能
function initDebugConsole() {
    const btnDebugConsole = document.getElementById('btn-debug-console');
    const debugConsoleOverlay = document.getElementById('debug-console-overlay');
    const btnCloseConsole = document.getElementById('btn-close-console');
    const btnClearConsole = document.getElementById('btn-clear-console');
    const btnCopyConsole = document.getElementById('btn-copy-console');
    const debugConsoleOutput = document.getElementById('debug-console-output');
    const filterLog = document.getElementById('console-filter-log');
    const filterWarn = document.getElementById('console-filter-warn');
    const filterError = document.getElementById('console-filter-error');
    
    if (!isDebugConsoleEnabled) {
        if (btnDebugConsole) {
            btnDebugConsole.style.display = 'none';
        }
        return;
    }
    
    if (!btnDebugConsole || !debugConsoleOverlay) {
        originalConsole.warn("调试控制台元素未找到");
        return;
    }
    
    // 拦截 console 方法（使用 originalConsole 避免递归）
    const originalLog = originalConsole.log;
    const originalWarn = originalConsole.warn;
    const originalError = originalConsole.error;
    const originalInfo = originalConsole.info;
    
    console.log = function(...args) {
        originalLog.apply(console, args);
        addDebugLog('log', args);
    };
    
    console.warn = function(...args) {
        originalWarn.apply(console, args);
        addDebugLog('warn', args);
    };
    
    console.error = function(...args) {
        originalError.apply(console, args);
        addDebugLog('error', args);
    };
    
    console.info = function(...args) {
        originalInfo.apply(console, args);
        addDebugLog('info', args);
    };
    
    // 添加日志到控制台
    function addDebugLog(type, args) {
        const timestamp = new Date().toLocaleTimeString();
        const message = args.map(arg => {
            if (typeof arg === 'object') {
                try {
                    return JSON.stringify(arg, null, 2);
                } catch (e) {
                    return String(arg);
                }
            }
            return String(arg);
        }).join(' ');
        
        debugConsoleLogs.push({ type, message, timestamp });
        
        // 限制日志数量
        if (debugConsoleLogs.length > debugConsoleMaxLogs) {
            debugConsoleLogs.shift();
        }
        
        // 如果控制台已打开，更新显示
        if (debugConsoleOverlay.style.display !== 'none') {
            updateDebugConsoleOutput();
        }
    }
    
    // 更新控制台输出
    function updateDebugConsoleOutput() {
        if (!debugConsoleOutput) return;
        
        const showLog = filterLog ? filterLog.checked : true;
        const showWarn = filterWarn ? filterWarn.checked : true;
        const showError = filterError ? filterError.checked : true;
        
        const filteredLogs = debugConsoleLogs.filter(log => {
            if (log.type === 'log' || log.type === 'info') return showLog;
            if (log.type === 'warn') return showWarn;
            if (log.type === 'error') return showError;
            return true;
        });
        
        debugConsoleOutput.innerHTML = filteredLogs.map(log => {
            return `<div class="debug-console-log ${log.type}">
                <span class="debug-console-timestamp">[${log.timestamp}]</span>
                ${escapeHtml(log.message)}
            </div>`;
        }).join('');
        
        // 自动滚动到底部
        debugConsoleOutput.scrollTop = debugConsoleOutput.scrollHeight;
    }
    
    // HTML 转义
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    // 打开控制台
    function openDebugConsole() {
        debugConsoleOverlay.style.display = 'flex';
        updateDebugConsoleOutput();
    }
    
    // 关闭控制台
    function closeDebugConsole() {
        debugConsoleOverlay.style.display = 'none';
    }
    
    // 清空控制台
    function clearDebugConsole() {
        debugConsoleLogs = [];
        if (debugConsoleOutput) {
            debugConsoleOutput.innerHTML = '';
        }
    }
    
    // 复制控制台内容
    function copyDebugConsole() {
        const text = debugConsoleLogs.map(log => {
            return `[${log.timestamp}] [${log.type.toUpperCase()}] ${log.message}`;
        }).join('\n');
        
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(() => {
                alert('日志已复制到剪贴板');
            }).catch(() => {
                fallbackCopyText(text);
            });
        } else {
            fallbackCopyText(text);
        }
    }
    
    // 备用复制方法
    function fallbackCopyText(text) {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.opacity = '0';
        document.body.appendChild(textArea);
        textArea.select();
        try {
            document.execCommand('copy');
            alert('日志已复制到剪贴板');
        } catch (e) {
            alert('复制失败，请手动选择文本');
        }
        document.body.removeChild(textArea);
    }
    
    // 绑定事件
    if (btnDebugConsole) {
        btnDebugConsole.onclick = openDebugConsole;
    }
    
    if (btnCloseConsole) {
        btnCloseConsole.onclick = closeDebugConsole;
    }
    
    if (btnClearConsole) {
        btnClearConsole.onclick = clearDebugConsole;
    }
    
    if (btnCopyConsole) {
        btnCopyConsole.onclick = copyDebugConsole;
    }
    
    // 过滤器变化时更新显示
    if (filterLog) filterLog.addEventListener('change', updateDebugConsoleOutput);
    if (filterWarn) filterWarn.addEventListener('change', updateDebugConsoleOutput);
    if (filterError) filterError.addEventListener('change', updateDebugConsoleOutput);
    
    // 点击遮罩层关闭
    debugConsoleOverlay.addEventListener('click', (e) => {
        if (e.target === debugConsoleOverlay) {
            closeDebugConsole();
        }
    });
    
    console.log("🐛 调试控制台已初始化");
}

// 湖中仙女公开结果按钮事件
