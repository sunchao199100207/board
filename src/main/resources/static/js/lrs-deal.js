(function () {
    var params = new URLSearchParams(window.location.search);
    var auto = params.get('auto') === '1';
    var presetRole = params.get('role') || '';
    var presetRoom = params.get('room') || '';
    var presetSeat = parseInt(params.get('seat') || '0', 10) || 0;
    var presetPlayers = parseInt(params.get('players') || '0', 10) || 0;
    if (!presetPlayers && params.get('preset') === 'small') {
        presetPlayers = 6;
    }
    var delayMs = parseInt(params.get('delayMs') || '300', 10) || 300;
    var wantJudge = presetRole === 'judge' || presetRole === 'host';

    var SINGLE_ROLES = [
        ['wolfKing', '黑狼王'],
        ['whiteWolfKing', '白狼王'],
        ['seer', '预言家'],
        ['witch', '女巫'],
        ['hunter', '猎人'],
        ['guard', '守卫'],
        ['thief', '盗贼'],
        ['knight', '骑士'],
        ['silenceElder', '禁言长老']
    ];

    var presetPid = params.get('pid') || '';
    var stompClient = null;
    var socketGeneration = 0;
    var reconnectTimer = null;
    var playerId = /^[A-Za-z0-9-]{8,64}$/.test(presetPid) ? presetPid : localStorage.getItem('wolfPlayerId');
    var state = null;
    var scriptIndex = 0;
    var cardHidden = false;
    var subscribed = false;
    var autoSeatSent = false;
    var autoReadySent = false;
    var autoJudgeSent = false;
    var autoConfigSent = false;
    var autoSeatTries = 0;
    var joinedThisPage = false;

    if (!playerId) {
        playerId = createId();
        localStorage.setItem('wolfPlayerId', playerId);
    }
    if (presetRoom) {
        document.getElementById('room-number').value = presetRoom;
    }

    document.getElementById('btn-enter').onclick = enterRoom;
    document.getElementById('judge-seat').onclick = claimJudge;
    document.getElementById('btn-script-prev').onclick = function () { moveScript(-1); };
    document.getElementById('btn-script-next').onclick = function () { moveScript(1); };
    window.addEventListener('message', onParentMessage);
    connect();

    function createId() {
        if (window.crypto && crypto.randomUUID) {
            return crypto.randomUUID();
        }
        return 'wolf-' + Math.random().toString(16).slice(2) + Date.now().toString(16);
    }

    function connect() {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            showError('home-error', '页面资源加载不全');
            return;
        }
        var generation = ++socketGeneration;
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
            reconnectTimer = null;
        }
        if (stompClient) {
            var previous = stompClient;
            stompClient = null;
            try {
                previous.disconnect(function () {});
            } catch (e) {}
        }
        var socket = new SockJS('ws');
        var client = Stomp.over(socket);
        stompClient = client;
        client.debug = function () {};
        client.connect({}, function () {
            if (generation !== socketGeneration) {
                return;
            }
            subscribed = true;
            client.subscribe('/topic/wolf/player/' + playerId, onPush);
            resumeRoom();
        }, function () {
            if (generation !== socketGeneration) {
                return;
            }
            subscribed = false;
            var target = document.getElementById('page-room').classList.contains('active') ? 'room-error' : 'home-error';
            showError(target, '连接断开了，正在回到房间');
            scheduleReconnect();
        });
    }

    function scheduleReconnect() {
        if (reconnectTimer) {
            return;
        }
        reconnectTimer = setTimeout(function () {
            reconnectTimer = null;
            connect();
        }, 1200);
    }

    function resumeRoom() {
        if (!auto) {
            if (!joinedThisPage) {
                return;
            }
            var savedRoom = localStorage.getItem('wolfRoom');
            if (savedRoom) {
                send('/app/wolf/enter', { roomNumber: savedRoom });
            }
            return;
        }
        setTimeout(function () {
            document.getElementById('room-number').value = presetRoom || savedRoom || '6666';
            enterRoom();
        }, delayMs);
    }

    function send(destination, body) {
        if (!stompClient || !stompClient.connected) {
            showError('home-error', '还没连上，请稍等');
            return;
        }
        body.playerId = playerId;
        stompClient.send(destination, {}, JSON.stringify(body));
    }

    function enterRoom() {
        var roomNumber = document.getElementById('room-number').value.trim();
        if (!/^\d{4,6}$/.test(roomNumber)) {
            showError('home-error', '房间号需要 4 到 6 位数字');
            return;
        }
        showError('home-error', '');
        send('/app/wolf/enter', { roomNumber: roomNumber });
    }

    function claimJudge() {
        if (!state || state.dealt || state.host) {
            return;
        }
        if (state.judgeTaken && !window.confirm('接过法官位？对方会回到号码席，角色开关交给你。')) {
            return;
        }
        showError('room-error', '');
        send('/app/wolf/claim-judge', { roomNumber: state.roomNumber });
    }

    function onPush(frame) {
        var push = JSON.parse(frame.body);
        if (push.type === 'error') {
            if (push.error === '已经发牌，不能改配置') {
                return;
            }
            var target = document.getElementById('page-room').classList.contains('active') ? 'room-error' : 'home-error';
            showError(target, push.error || '操作失败');
            retryAutoSeat(push.error || '');
            return;
        }
        if (push.type === 'closed') {
            localStorage.removeItem('wolfRoom');
            state = null;
            show('page-home');
            showError('home-error', push.error || '已离开房间');
            return;
        }
        if (push.type === 'state' && push.state) {
            showError('home-error', '');
            showError('room-error', '');
            localStorage.setItem('wolfRoom', push.state.roomNumber);
            joinedThisPage = true;
            render(push.state);
            runAuto(push.state);
        }
    }

    function render(next) {
        var previous = state;
        var freshDeal = !previous || previous.dealt !== next.dealt || previous.round !== next.round;
        reconcileJudgeMarks(previous, next);
        state = next;
        if (freshDeal) {
            scriptIndex = 0;
            cardHidden = localStorage.getItem(coverStorageKey(next.roomNumber, next.round)) === '1';
        }
        show('page-room');
        document.getElementById('page-room').classList.toggle('player-card-only', !next.host && !!next.card);
        document.getElementById('room-info-number').textContent = next.roomNumber;
        paintJudge(next);
        paintPrompt(next);
        paintSeats(next);
        paintDeal(next);
        paintActions(next);
        paintConfig(next);
        paintScript(next);
        var status = document.getElementById('room-status-text');
        if (next.dealt) {
            status.textContent = '';
            status.style.display = 'none';
        } else {
            status.style.display = '';
            status.textContent = '玩家: ' + next.seated + ' / ' + next.total + ' | 已准备: ' + next.readyCount + ' / ' + next.total;
        }
    }

    function paintJudge(next) {
        var seat = document.getElementById('judge-seat');
        seat.className = 'seat judge-seat';
        if (next.host) {
            seat.classList.add('selected');
        } else if (next.judgeTaken) {
            seat.classList.add('taken');
            if (!next.dealt) {
                seat.classList.add('switchable');
            }
        }
        seat.querySelector('.seat-number').textContent = '法官';
    }

    function paintPrompt(next) {
        var prompt = document.getElementById('room-prompt');
        var card = document.getElementById('my-card');
        if (!next.host && next.card) {
            prompt.style.display = 'none';
            card.style.display = 'block';
            card.classList.toggle('wolf', next.card.camp === 'wolf');
            card.classList.toggle('good', next.card.camp !== 'wolf');
            card.classList.toggle('covered', cardHidden);
            document.getElementById('my-card-image').src = imageSrc(next.card.imageUrl);
            document.getElementById('my-card-image').alt = next.card.roleName;
            document.getElementById('my-card-role').textContent = cardHidden ? '' : next.card.roleName;
            document.getElementById('my-card-hint').textContent = cardHidden ? '' : next.card.hint;
            return;
        }
        card.style.display = 'none';
        if (next.host && next.dealt) {
            prompt.style.display = 'none';
            prompt.textContent = '';
            return;
        }
        prompt.style.display = 'block';
        if (next.host) {
            prompt.textContent = '你是法官。点下面的空位可以下来换座，开关角色后等人齐了再发牌。';
        } else if (next.mySeat) {
            prompt.textContent = '点法官位可以换人。点其他空位可以换座。';
        } else if (!next.judgeTaken) {
            prompt.textContent = '最上方法官位，下面是号码。先坐下，再点准备。';
        } else {
            prompt.textContent = '请选择你的座位，然后点击准备';
        }
    }

    function paintSeats(next) {
        var grid = document.getElementById('seat-grid');
        grid.style.display = next.host && next.dealt ? 'none' : 'grid';
        grid.innerHTML = '';
        (next.seats || []).forEach(function (info) {
            var seat = document.createElement('div');
            seat.className = 'seat';
            if (info.self) {
                seat.classList.add('selected');
            } else if (info.occupied) {
                seat.classList.add('taken');
            } else if (!next.dealt) {
                seat.classList.add('pickable');
            }
            if (info.ready) {
                seat.classList.add('ready');
            }
            var number = document.createElement('span');
            number.className = 'seat-number';
            number.textContent = String(info.seat);
            seat.appendChild(number);
            if (info.emoji) {
                var mark = document.createElement('span');
                mark.className = 'seat-role';
                mark.textContent = info.emoji;
                seat.appendChild(mark);
            }
            seat.onclick = function () { onSeatClick(info); };
            grid.appendChild(seat);
        });
    }

    function paintDeal(next) {
        var list = document.getElementById('deal-list');
        if (!next.host || !next.dealt) {
            list.style.display = 'none';
            list.innerHTML = '';
            return;
        }
        list.style.display = 'grid';
        list.innerHTML = '';
        (next.seats || []).forEach(function (info) {
            var card = document.createElement('figure');
            card.className = 'portrait deal-card ' + (info.camp === 'wolf' ? 'wolf' : 'good');
            var image = document.createElement('img');
            image.src = imageSrc('img/lrs/' + info.roleName + '.jpg');
            image.alt = info.roleName || '';
            var badge = document.createElement('span');
            badge.className = 'portrait-seat';
            var dead = isDead(next, info.seat);
            badge.textContent = info.seat + '号';
            var caption = document.createElement('figcaption');
            caption.textContent = info.roleName || '';
            var icon = document.createElement('div');
            icon.className = 'icon-wrap';
            icon.appendChild(image);
            if (dead) {
                card.classList.add('dead');
                var stamp = document.createElement('div');
                stamp.className = 'dead-stamp';
                stamp.textContent = '死';
                icon.appendChild(stamp);
            }
            if (info.roleName === '女巫') {
                icon.appendChild(witchPotions(next));
            }
            var meta = document.createElement('div');
            meta.className = 'card-meta';
            meta.appendChild(badge);
            meta.appendChild(caption);
            card.appendChild(icon);
            card.appendChild(meta);
            card.onclick = function () { toggleDead(next, info.seat); };
            list.appendChild(card);
        });
    }

    function reconcileJudgeMarks(previous, next) {
        if (!next.host) {
            return;
        }
        var deadKey = deadStorageKey(next);
        var potion = potionKey(next);
        var sigKey = 'wolfDealSig:' + next.roomNumber + ':' + next.round;
        if (!next.dealt) {
            localStorage.removeItem(deadKey);
            localStorage.removeItem(potion);
            localStorage.removeItem(sigKey);
            return;
        }
        var sig = (next.seats || []).map(function (info) {
            return info.seat + '=' + (info.roleName || '');
        }).join(',');
        var redeal = previous && (!previous.dealt || previous.round !== next.round || previous.roomNumber !== next.roomNumber);
        if (redeal || localStorage.getItem(sigKey) !== sig) {
            localStorage.removeItem(deadKey);
            localStorage.removeItem(potion);
            localStorage.setItem(sigKey, sig);
        }
    }

    function deadStorageKey(next) {
        return 'wolfDead:' + next.roomNumber + ':' + next.round;
    }

    function readDead(next) {
        try {
            return JSON.parse(localStorage.getItem(deadStorageKey(next)) || '{}');
        } catch (e) {
            return {};
        }
    }

    function isDead(next, seat) {
        return !!readDead(next)[seat];
    }

    function toggleDead(next, seat) {
        var marks = readDead(next);
        if (marks[seat]) {
            delete marks[seat];
        } else {
            marks[seat] = 1;
        }
        localStorage.setItem(deadStorageKey(next), JSON.stringify(marks));
        paintDeal(state);
    }

    function potionKey(next) {
        return 'wolfPotion:' + next.roomNumber + ':' + next.round;
    }

    function readPotion(next) {
        try {
            var saved = JSON.parse(localStorage.getItem(potionKey(next)) || '{}');
            return {
                antidote: saved.antidote !== false,
                poison: saved.poison !== false
            };
        } catch (e) {
            return { antidote: true, poison: true };
        }
    }

    function witchPotions(next) {
        var stock = readPotion(next);
        var box = document.createElement('div');
        box.className = 'witch-pots';
        box.appendChild(potionChip('解药', stock.antidote, 'antidote'));
        box.appendChild(potionChip('毒药', stock.poison, 'poison'));
        return box;
    }

    function potionChip(name, left, key) {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'potion-chip ' + key + (left ? ' left' : ' used');
        button.textContent = name;
        button.onclick = function (event) {
            event.stopPropagation();
            var latest = readPotion(state);
            latest[key] = !latest[key];
            localStorage.setItem(potionKey(state), JSON.stringify(latest));
            paintDeal(state);
        };
        return button;
    }

    function imageSrc(url) {
        if (!url) {
            return '';
        }
        if (url.indexOf('img/lrs/') >= 0 && url.slice(-4) === '.png') {
            url = url.slice(0, -4) + '.jpg';
        }
        var slash = url.lastIndexOf('/');
        if (slash < 0) {
            return encodeURIComponent(url);
        }
        return url.slice(0, slash + 1) + encodeURIComponent(url.slice(slash + 1)) + '?v=s2';
    }

    function paintActions(next) {
        var actions = document.getElementById('room-actions');
        var bar = document.getElementById('bar-actions');
        actions.innerHTML = '';
        if (bar) {
            bar.innerHTML = '';
            bar.style.display = 'none';
        }
        if (next.host && next.dealt && bar) {
            actions.style.display = 'none';
            bar.style.display = 'flex';
            bar.appendChild(actionButton('再发一局', 'btn-primary', true, function () {
                if (window.confirm('再洗一次牌？玩家需要重新准备。')) {
                    send('/app/wolf/new-round', { roomNumber: next.roomNumber });
                }
            }));
            bar.appendChild(actionButton('解散房间', 'btn-danger', true, function () {
                if (window.confirm('解散后这副牌就作废了。')) {
                    send('/app/wolf/close', { roomNumber: next.roomNumber });
                }
            }));
            return;
        }
        actions.style.display = '';
        if (next.host && !next.dealt) {
            actions.appendChild(actionButton(next.canDeal ? '发牌' : '等待准备', 'btn-primary', next.canDeal, function () {
                send('/app/wolf/deal', { roomNumber: next.roomNumber });
            }));
            actions.appendChild(actionButton('解散房间', 'btn-danger', true, function () {
                if (window.confirm('解散后这副牌就作废了。')) {
                    send('/app/wolf/close', { roomNumber: next.roomNumber });
                }
            }));
            return;
        }
        if (!next.dealt) {
            actions.appendChild(actionButton('离开房间', 'btn-danger', true, leaveRoom));
            var readyBtn = actionButton(next.ready ? '取消准备' : '准备', next.ready ? 'btn-secondary' : 'btn-primary', !!next.mySeat, function () {
                send('/app/wolf/ready', { roomNumber: next.roomNumber, ready: !next.ready });
            });
            actions.appendChild(readyBtn);
            return;
        }
        actions.appendChild(actionButton(cardHidden ? '查看身份' : '遮住身份', 'btn-primary', true, function () {
            cardHidden = !cardHidden;
            localStorage.setItem(coverStorageKey(state.roomNumber, state.round), cardHidden ? '1' : '0');
            paintPrompt(state);
            paintActions(state);
        }));
        actions.appendChild(actionButton('离开房间', 'btn-secondary', true, leaveRoom));
    }

    function paintConfig(next) {
        var box = document.getElementById('role-config');
        if (!next.host || next.dealt || !next.config) {
            box.style.display = 'none';
            return;
        }
        box.style.display = 'block';
        box.innerHTML = '';
        box.appendChild(countRow('平民', 'civilian', next.config.civilian));
        box.appendChild(countRow('狼人', 'werewolf', next.config.werewolf));
        SINGLE_ROLES.forEach(function (item) {
            box.appendChild(toggleRow(item[1], item[0], !!next.config[item[0]]));
        });
        var total = document.createElement('div');
        total.style.textAlign = 'center';
        total.style.marginTop = '6px';
        total.textContent = '号码 ' + next.total + ' 个';
        box.appendChild(total);
    }

    function countRow(label, key, value) {
        var row = document.createElement('div');
        row.className = 'role-config-row';
        var name = document.createElement('span');
        name.textContent = label;
        var controls = document.createElement('span');
        controls.className = 'stepper';
        controls.appendChild(tinyButton('-', function () { changeCount(key, -1); }));
        var count = document.createElement('b');
        count.textContent = ' ' + value + ' ';
        controls.appendChild(count);
        controls.appendChild(tinyButton('+', function () { changeCount(key, 1); }));
        row.appendChild(name);
        row.appendChild(controls);
        return row;
    }

    function toggleRow(label, key, on) {
        var row = document.createElement('div');
        row.className = 'role-config-row';
        var name = document.createElement('span');
        name.textContent = label;
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'role-switch' + (on ? ' on' : '');
        button.setAttribute('aria-pressed', on ? 'true' : 'false');
        button.setAttribute('aria-label', label);
        button.onclick = function () {
            var config = Object.assign({}, state.config);
            config[key] = !on;
            send('/app/wolf/config', { roomNumber: state.roomNumber, config: config });
        };
        row.appendChild(name);
        row.appendChild(button);
        return row;
    }

    function tinyButton(text, onClick) {
        var button = document.createElement('button');
        button.type = 'button';
        button.textContent = text;
        button.onclick = onClick;
        return button;
    }

    function changeCount(key, delta) {
        if (!state || !state.config) {
            return;
        }
        var config = Object.assign({}, state.config);
        var next = (config[key] || 0) + delta;
        if (next < 0 || next > 8) {
            return;
        }
        config[key] = next;
        send('/app/wolf/config', { roomNumber: state.roomNumber, config: config });
    }

    function paintScript(next) {
        var panel = document.getElementById('script-panel');
        if (!next.host || !next.dealt || !next.script || !next.script.length) {
            panel.style.display = 'none';
            return;
        }
        panel.style.display = 'block';
        var step = next.script[scriptIndex] || next.script[0];
        document.getElementById('script-title').textContent = step.title;
        document.getElementById('script-text').textContent = step.text;
        document.getElementById('script-progress').textContent = (scriptIndex + 1) + ' / ' + next.script.length;
    }

    function moveScript(delta) {
        if (!state || !state.script || !state.script.length) {
            return;
        }
        scriptIndex = Math.max(0, Math.min(state.script.length - 1, scriptIndex + delta));
        paintScript(state);
    }

    function onSeatClick(info) {
        if (!state || state.dealt) {
            return;
        }
        if (info.self) {
            showError('room-error', '');
            send('/app/wolf/select-seat', { roomNumber: state.roomNumber, seatNumber: 0 });
            return;
        }
        if (info.occupied) {
            showError('room-error', '这个座位已经有人了');
            return;
        }
        showError('room-error', '');
        send('/app/wolf/select-seat', { roomNumber: state.roomNumber, seatNumber: info.seat });
    }

    function coverStorageKey(roomNumber, round) {
        return 'wolfCover:' + roomNumber + ':' + round;
    }

    function leaveRoom() {
        send('/app/wolf/leave', { roomNumber: state ? state.roomNumber : localStorage.getItem('wolfRoom') });
    }

    function runAuto(next) {
        if (!auto) {
            return;
        }
        if (wantJudge && !next.host && !autoJudgeSent) {
            autoJudgeSent = true;
            send('/app/wolf/claim-judge', { roomNumber: next.roomNumber });
            return;
        }
        if (wantJudge && next.host && presetPlayers >= 6 && !next.dealt && !autoConfigSent) {
            autoConfigSent = true;
            send('/app/wolf/config', {
                roomNumber: next.roomNumber,
                config: configForPlayers(presetPlayers)
            });
            return;
        }
        if (wantJudge || next.host || next.dealt) {
            return;
        }
        if (!next.mySeat && presetSeat > 0 && !autoSeatSent) {
            autoSeatSent = true;
            send('/app/wolf/select-seat', { roomNumber: next.roomNumber, seatNumber: presetSeat });
            return;
        }
        if (next.mySeat && !next.ready && !autoReadySent) {
            autoReadySent = true;
            send('/app/wolf/ready', { roomNumber: next.roomNumber, ready: true });
        }
    }

    /**
     * 按人数配一副能发的牌：狼人少于好人，6 人沿用 3 平民、2 狼人、预言家。
     */
    function configForPlayers(count) {
        var n = count < 6 ? 6 : (count > 12 ? 12 : count);
        var config = {
            civilian: 0,
            werewolf: 0,
            wolfKing: false,
            whiteWolfKing: false,
            seer: true,
            witch: false,
            hunter: false,
            guard: false,
            thief: false,
            knight: false,
            silenceElder: false
        };
        if (n === 6) {
            config.werewolf = 2;
            config.civilian = 3;
            return config;
        }
        var wolves = n >= 12 ? 4 : (n >= 9 ? 3 : 2);
        var godKeys = ['witch', 'hunter', 'guard', 'knight', 'thief', 'silenceElder'];
        var extraGods = Math.min(godKeys.length, Math.max(0, n - wolves - 1 - 2));
        config.werewolf = wolves;
        if (n >= 10) {
            config.wolfKing = true;
            config.werewolf = wolves - 1;
        }
        for (var i = 0; i < extraGods; i++) {
            config[godKeys[i]] = true;
        }
        config.civilian = n - config.werewolf - (config.wolfKing ? 1 : 0) - 1 - extraGods;
        return config;
    }

    function retryAutoSeat(message) {
        if (!auto || wantJudge || !presetSeat || autoSeatTries >= 8) {
            return;
        }
        if (message.indexOf('座位') < 0) {
            return;
        }
        autoSeatSent = false;
        autoSeatTries += 1;
        setTimeout(function () {
            if (state && !state.mySeat && !state.host && !state.dealt) {
                runAuto(state);
            }
        }, 500);
    }

    function onParentMessage(event) {
        var originOk = event.origin === window.location.origin || event.origin === '' || event.origin === 'null';
        if (!originOk || !event.data || !state) {
            return;
        }
        if (event.data.type === 'WOLF_DEAL' && state.host) {
            send('/app/wolf/deal', { roomNumber: state.roomNumber });
        }
        if (event.data.type === 'WOLF_READY' && !state.host && state.mySeat && !state.dealt) {
            send('/app/wolf/ready', { roomNumber: state.roomNumber, ready: !state.ready });
        }
    }

    function actionButton(text, className, enabled, onClick) {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn ' + className;
        button.textContent = text;
        button.disabled = !enabled;
        button.onclick = onClick;
        return button;
    }

    function show(id) {
        document.querySelectorAll('.page').forEach(function (page) {
            page.classList.toggle('active', page.id === id);
        });
    }

    function showError(id, message) {
        var el = document.getElementById(id);
        if (!el) {
            return;
        }
        el.textContent = message || '';
        el.style.display = message ? 'block' : 'none';
    }
})();
