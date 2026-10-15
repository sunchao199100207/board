(function () {
    var GUIDE = [
        {
            title: '一、你要拿两张身份牌',
            body: '<p>游戏开始时，你会拿到两张身份牌。</p><p>两张牌分上下摆放：</p><p><strong>上身份</strong>：你现在的身份，别人看到的是这个，你用的也是这个技能。</p><p><strong>下身份</strong>：藏起来的身份，暂时不能用。只有上身份出局后，下身份才会激活。</p><p>简单记：上面是现在，下面是以后。</p>'
        },
        {
            title: '二、你属于哪个阵营？',
            body: '<p>看你的两张牌里有没有狼人牌。狼王也算狼人牌。</p><p>只要有一张是狼人牌 → 你就是<strong>狼人阵营</strong>。</p><p>两张都不是狼人牌 → 你就是<strong>好人阵营</strong>。</p><p>注意：即使狼人牌在下面、上面是好人，你仍然是狼人阵营，只是暂时不能刀人、也不认识狼队友。</p>'
        },
        {
            title: '三、夜晚会发生什么？',
            body: '<p>夜晚按顺序进行，你只需要在轮到你的时候睁眼或行动。</p><p><strong>狼人行动</strong>：狼牌在上身份，今晚可以睁眼，和狼队友一起选人刀。狼牌在下身份，今晚不能睁眼，也不能刀人，继续装睡。</p><p><strong>预言家验人</strong>：只能查验目标的上身份。验到「好人」不一定是真好人，他下面可能藏着狼。</p><p><strong>女巫、猎人、守卫、禁言长老、骑士</strong>：只能用上身份的技能。</p><p><strong>金宝宝</strong>：两张牌都是平民。狼人杀光所有金宝宝，或者杀光所有神职，狼人获胜。</p>'
        },
        {
            title: '四、白天会发生什么？',
            body: '<p>白天和普通狼人杀一样：公布昨晚谁出局，大家发言，投票放逐一名玩家。</p><p><strong>关键区别：</strong>一个人出局后，如果他有下身份，下身份会激活，他不会真正离开游戏，而是换成下身份继续玩。</p><p>所以白天投出去的人，不一定是真的死了。</p>'
        },
        {
            title: '五、出局后怎么算？',
            body: '<p>上身份出局 → 下身份激活，你继续游戏，身份按新的上身份来行动。</p><p>下身份也出局 → 你才真正离开游戏。</p><p>如果一个人上身份是好人、下身份是狼：上身份出局后，他变成狼人，夜晚可以刀人。</p><p>如果场上其他狼都死了，只要他这张狼人牌还在，游戏就没结束。</p>'
        },
        {
            title: '六、怎么算赢？',
            body: '<p><strong>好人获胜：</strong>所有狼人牌都被消灭（不管在上身份还是下身份）。</p><p><strong>狼人获胜：</strong>杀光所有神职牌，或者杀光所有金宝宝。</p><p>记住：看的是身份牌，不是玩家。只要还有一张狼人牌没被消灭，狼人阵营就还在。</p>'
        },
        {
            title: '七、最容易搞错的 3 件事',
            body: '<p><strong>「我上面是好人，我就是好人。」</strong>错。只要下面有狼，你就是狼人阵营。</p><p><strong>「预言家验我是好人，那我肯定是好人。」</strong>不一定。预言家只能验上身份，你下面可能是狼。</p><p><strong>「其他狼都死了，好人赢了。」</strong>不一定。只要还有一张狼人牌藏在某人下身份里，游戏就继续。</p>'
        }
    ];

    var params = new URLSearchParams(window.location.search);
    var auto = params.get('auto') === '1';
    var presetRole = params.get('role') || '';
    var presetRoom = params.get('room') || '';
    var presetSeat = parseInt(params.get('seat') || '0', 10) || 0;
    var presetPlayers = parseInt(params.get('players') || '0', 10) || 0;
    var delayMs = parseInt(params.get('delayMs') || '300', 10) || 300;
    var wantJudge = presetRole === 'judge' || presetRole === 'host';
    var presetPid = params.get('pid') || '';

    var guideIndex = 0;
    var playerId = /^[A-Za-z0-9-]{8,64}$/.test(presetPid) ? presetPid : localStorage.getItem('dualPlayerId');
    var stompClient = null;
    var state = null;
    var joinedThisPage = false;
    var cardHidden = false;
    var pickIndex = 0;
    var socketGeneration = 0;
    var reconnectTimer = null;
    var autoSeatSent = false;
    var autoReadySent = false;
    var autoJudgeSent = false;
    var autoConfigSent = false;
    var autoSeatTries = 0;

    if (!playerId) {
        playerId = (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : ('dual-' + Date.now().toString(16) + Math.random().toString(16).slice(2));
        localStorage.setItem('dualPlayerId', playerId);
    }
    if (auto && presetRoom) {
        document.getElementById('room-number').value = presetRoom;
    }

    document.getElementById('btn-enter').onclick = enterRoom;
    document.getElementById('judge-seat').onclick = claimJudge;
    document.getElementById('guide-prev').onclick = function () { stepGuide(-1); };
    document.getElementById('guide-next').onclick = function () { stepGuide(1); };
    window.addEventListener('message', onParentMessage);
    paintGuide();
    connect();

    function paintGuide() {
        var step = GUIDE[guideIndex];
        document.getElementById('guide-title').textContent = step.title;
        document.getElementById('guide-progress').textContent = (guideIndex + 1) + ' / ' + GUIDE.length;
        document.getElementById('guide-body').innerHTML = step.body;
        document.getElementById('guide-prev').disabled = guideIndex === 0;
        document.getElementById('guide-next').textContent = guideIndex === GUIDE.length - 1 ? '回到开头' : '下一则';
    }

    function stepGuide(delta) {
        if (delta > 0 && guideIndex === GUIDE.length - 1) {
            guideIndex = 0;
        } else {
            guideIndex = Math.max(0, Math.min(GUIDE.length - 1, guideIndex + delta));
        }
        paintGuide();
    }

    function connect() {
        var generation = ++socketGeneration;
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
            reconnectTimer = null;
        }
        if (stompClient) {
            var previous = stompClient;
            stompClient = null;
            try { previous.disconnect(function () {}); } catch (e) {}
        }
        var client = Stomp.over(new SockJS('ws'));
        stompClient = client;
        client.debug = function () {};
        client.connect({}, function () {
            if (generation !== socketGeneration) return;
            client.subscribe('/topic/dual/player/' + playerId, onPush);
            if (auto) {
                setTimeout(function () {
                    document.getElementById('room-number').value = presetRoom || '6666';
                    enterRoom();
                }, delayMs);
                return;
            }
            if (joinedThisPage && localStorage.getItem('dualRoom')) {
                send('/app/dual/enter', { roomNumber: localStorage.getItem('dualRoom') });
            }
        }, function () {
            if (generation !== socketGeneration) return;
            var target = document.getElementById('page-room').classList.contains('active') ? 'room-error' : 'home-error';
            showError(target, '连接断开了，正在回到房间');
            reconnectTimer = setTimeout(connect, 1200);
        });
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
        send('/app/dual/enter', { roomNumber: roomNumber });
    }

    function claimJudge() {
        if (!state || state.dealt || state.host) return;
        if (state.judgeTaken && !window.confirm('接过法官位？对方会回到号码席。')) return;
        send('/app/dual/claim-judge', { roomNumber: state.roomNumber });
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
            localStorage.removeItem('dualRoom');
            state = null;
            show('page-home');
            showError('home-error', push.error || '已离开房间');
            return;
        }
        if (push.type === 'state' && push.state) {
            showError('home-error', '');
            showError('room-error', '');
            localStorage.setItem('dualRoom', push.state.roomNumber);
            joinedThisPage = true;
            render(push.state);
            runAuto(push.state);
        }
    }

    function runAuto(next) {
        if (!auto) {
            return;
        }
        if (wantJudge && !next.host && !autoJudgeSent) {
            autoJudgeSent = true;
            send('/app/dual/claim-judge', { roomNumber: next.roomNumber });
            return;
        }
        if (wantJudge && next.host && presetPlayers >= 6 && presetPlayers <= 8 && !next.dealt && !autoConfigSent) {
            autoConfigSent = true;
            if (!next.config || next.config.players !== presetPlayers) {
                send('/app/dual/config', { roomNumber: next.roomNumber, players: presetPlayers });
            }
            return;
        }
        if (wantJudge || next.host || next.dealt) {
            return;
        }
        if (!next.mySeat && presetSeat > 0 && !autoSeatSent) {
            autoSeatSent = true;
            send('/app/dual/select-seat', { roomNumber: next.roomNumber, seatNumber: presetSeat });
            return;
        }
        if (next.mySeat && !next.ready && !autoReadySent) {
            autoReadySent = true;
            send('/app/dual/ready', { roomNumber: next.roomNumber, ready: true });
        }
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
        if (event.data.type === 'DUAL_DEAL' && state.host && !state.dealt) {
            send('/app/dual/deal', { roomNumber: state.roomNumber });
        }
        if (event.data.type === 'DUAL_CHOOSE' && !state.host && state.hand && !state.hand.confirmed) {
            send('/app/dual/choose', { roomNumber: state.roomNumber, upperIndex: pickIndex });
        }
    }

    function render(next) {
        var previous = state;
        var fresh = !previous || previous.dealt !== next.dealt || previous.round !== next.round;
        reconcileJudgeMarks(previous, next);
        state = next;
        if (fresh) {
            cardHidden = false;
            pickIndex = 0;
        }
        show('page-room');
        document.getElementById('page-room').classList.toggle('hand-only', !next.host && !!next.hand);
        document.getElementById('room-info-number').textContent = next.roomNumber;
        paintJudge(next);
        paintPrompt(next);
        paintHand(next);
        paintSeats(next);
        paintDeal(next);
        paintActions(next);
        paintConfig(next);
        var confirmed = next.dealt ? (' | 已确认上身份 ' + next.confirmedCount + ' / ' + next.total) : '';
        document.getElementById('room-status-text').textContent =
            '玩家: ' + next.seated + ' / ' + next.total + ' | 已准备: ' + next.readyCount + ' / ' + next.total + confirmed;
    }

    function paintJudge(next) {
        var seat = document.getElementById('judge-seat');
        seat.className = 'seat judge-seat';
        if (next.host) seat.classList.add('selected');
        else if (next.judgeTaken && !next.dealt) seat.classList.add('taken', 'switchable');
        else if (next.judgeTaken) seat.classList.add('taken');
        seat.querySelector('.seat-number').textContent = '法官';
    }

    function paintPrompt(next) {
        var prompt = document.getElementById('room-prompt');
        if (!next.host && next.hand) {
            prompt.style.display = 'none';
            return;
        }
        prompt.style.display = 'block';
        if (next.host && next.dealt) prompt.textContent = '牌已发出。点这位玩家的牌可以标记死亡，再点一次复活。夜里仍由你口述。';
        else if (next.host) prompt.textContent = '先选人数。点空位可以下来换座。人齐并准备后发牌。';
        else if (next.mySeat) prompt.textContent = '点法官位可以换人。点其他空位可以换座。';
        else if (!next.judgeTaken) prompt.textContent = '最上方法官位，下面是号码。先坐下，再点准备。';
        else prompt.textContent = '请选择你的座位，然后点击准备';
    }

    function paintHand(next) {
        var box = document.getElementById('my-hand');
        if (!next.hand || next.host) {
            box.style.display = 'none';
            box.innerHTML = '';
            return;
        }
        box.style.display = 'block';
        box.classList.toggle('covered', cardHidden);
        box.innerHTML = '';
        var hand = next.hand;
        if (!hand.confirmed) {
            var row = document.createElement('div');
            row.className = 'dual-pick-row';
            row.appendChild(pickFace(hand.cardA, 0, '第一张'));
            row.appendChild(pickFace(hand.cardB, 1, '第二张'));
            box.appendChild(row);
            var tip = document.createElement('p');
            tip.className = 'dual-camp';
            tip.textContent = '点一张作为上身份，另一张就是下身份。' + hand.campTip;
            box.appendChild(tip);
            var actions = document.createElement('div');
            actions.className = 'action-buttons';
            actions.appendChild(button('确认上身份', 'btn-primary', true, function () {
                send('/app/dual/choose', { roomNumber: state.roomNumber, upperIndex: pickIndex });
            }));
            box.appendChild(actions);
            return;
        }
        var upper = hand.upperIndex === 1 ? hand.cardB : hand.cardA;
        var lower = hand.upperIndex === 1 ? hand.cardA : hand.cardB;
        var stack = document.createElement('div');
        stack.className = 'dual-stack';
        stack.appendChild(showFace(upper, '上身份', 'upper'));
        stack.appendChild(showFace(lower, '下身份', 'lower'));
        box.appendChild(stack);
        var camp = document.createElement('p');
        camp.className = 'dual-camp';
        camp.textContent = cardHidden ? '' : hand.campTip;
        box.appendChild(camp);
    }

    function pickFace(card, index, label) {
        var face = document.createElement('button');
        face.type = 'button';
        face.className = 'dual-pick' + (pickIndex === index ? ' on' : '');
        face.appendChild(faceMedia(card));
        var tag = document.createElement('div');
        tag.className = 'dual-label';
        tag.textContent = pickIndex === index ? '上身份' : label;
        face.appendChild(tag);
        var name = document.createElement('div');
        name.className = 'dual-name';
        name.textContent = card.roleName;
        face.appendChild(name);
        face.onclick = function () {
            pickIndex = index;
            paintHand(state);
        };
        return face;
    }

    function showFace(card, label, size) {
        var face = document.createElement('div');
        face.className = 'dual-pick ' + size;
        face.appendChild(faceMedia(card));
        var tag = document.createElement('div');
        tag.className = 'dual-label';
        tag.textContent = label;
        face.appendChild(tag);
        var name = document.createElement('div');
        name.className = 'dual-name';
        name.textContent = cardHidden ? '' : card.roleName;
        face.appendChild(name);
        return face;
    }

    function faceMedia(card) {
        if (!card.imageUrl) {
            var fallback = document.createElement('div');
            fallback.className = 'fallback-face';
            fallback.textContent = card.emoji + ' ' + card.roleName;
            return fallback;
        }
        var image = document.createElement('img');
        image.src = imageSrc(card.imageUrl);
        image.alt = card.roleName;
        image.onerror = function () {
            var fallback = document.createElement('div');
            fallback.className = 'fallback-face';
            fallback.textContent = card.emoji + ' ' + card.roleName;
            image.replaceWith(fallback);
        };
        return image;
    }

    function paintSeats(next) {
        var grid = document.getElementById('seat-grid');
        grid.style.display = next.host && next.dealt ? 'none' : 'grid';
        grid.innerHTML = '';
        (next.seats || []).forEach(function (info) {
            var seat = document.createElement('div');
            seat.className = 'seat';
            if (info.self) seat.classList.add('selected');
            else if (info.occupied) seat.classList.add('taken');
            else if (!next.dealt) seat.classList.add('pickable');
            if (info.ready) seat.classList.add('ready');
            var number = document.createElement('span');
            number.className = 'seat-number';
            number.textContent = String(info.seat);
            seat.appendChild(number);
            seat.onclick = function () { onSeatClick(info); };
            grid.appendChild(seat);
        });
    }

    function onSeatClick(info) {
        if (!state || state.dealt) return;
        if (info.self) {
            send('/app/dual/select-seat', { roomNumber: state.roomNumber, seatNumber: 0 });
            return;
        }
        if (info.occupied) {
            showError('room-error', '这个座位已经有人了');
            return;
        }
        showError('room-error', '');
        send('/app/dual/select-seat', { roomNumber: state.roomNumber, seatNumber: info.seat });
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
            card.className = 'portrait ' + ((info.campName || '').indexOf('狼') >= 0 ? 'wolf' : 'good');
            var dead = isDead(next, info.seat);
            if (dead) {
                card.classList.add('dead');
            }
            var upperKey = info.confirmed && info.upperIndex === 1 ? 'cardB' : 'cardA';
            var lowerKey = upperKey === 'cardA' ? 'cardB' : 'cardA';
            var row = document.createElement('div');
            row.className = 'pair-row';
            row.appendChild(mini(info, upperKey, info.confirmed ? '上' : '甲'));
            row.appendChild(mini(info, lowerKey, info.confirmed ? '下' : '乙'));
            card.appendChild(row);
            var caption = document.createElement('figcaption');
            var upperName = info.confirmed ? (info.upperIndex === 1 ? info.cardBName : info.cardAName) : '未选上身份';
            caption.textContent = info.seat + '号 ' + (dead ? '已死亡 · ' : '') + (info.campName || '') + ' · ' + upperName;
            card.appendChild(caption);
            if (dead) {
                var stamp = document.createElement('div');
                stamp.className = 'dead-stamp';
                stamp.textContent = '死';
                card.appendChild(stamp);
            }
            card.onclick = function () { toggleDead(next, info.seat); };
            list.appendChild(card);
        });
    }

    function mini(info, which, tag) {
        var name = which === 'cardA' ? info.cardAName : info.cardBName;
        var emoji = which === 'cardA' ? info.cardAEmoji : info.cardBEmoji;
        var images = {
            '狼人': '狼人.jpg',
            '狼王': '黑狼王.jpg',
            '平民': '平民.jpg',
            '预言家': '预言家.jpg',
            '女巫': '女巫.jpg',
            '猎人': '猎人.jpg',
            '守卫': '守卫.jpg',
            '骑士': '骑士.jpg',
            '禁言长老': '禁言长老.jpg'
        };
        var box = document.createElement('div');
        if (images[name]) {
            var image = document.createElement('img');
            image.src = imageSrc('img/lrs/' + images[name]);
            image.alt = name || '';
            box.appendChild(image);
        } else {
            var face = document.createElement('div');
            face.className = 'fallback-face mini-face';
            face.textContent = (emoji || '') + (name || '');
            box.appendChild(face);
        }
        var label = document.createElement('div');
        label.className = 'dual-label';
        label.textContent = tag + ' ' + (name || '');
        box.appendChild(label);
        return box;
    }

    function paintActions(next) {
        var actions = document.getElementById('room-actions');
        actions.innerHTML = '';
        if (next.host && !next.dealt) {
            actions.appendChild(button(next.canDeal ? '发牌' : '等待准备', 'btn-primary', next.canDeal, function () {
                send('/app/dual/deal', { roomNumber: next.roomNumber });
            }));
            actions.appendChild(button('解散房间', 'btn-danger', true, function () {
                if (window.confirm('解散后这副牌就作废了。')) send('/app/dual/close', { roomNumber: next.roomNumber });
            }));
            return;
        }
        if (next.host && next.dealt) {
            actions.appendChild(button('再发一局', 'btn-primary', true, function () {
                if (window.confirm('再发一次？玩家需要重新准备，并重新选上身份。')) {
                    send('/app/dual/new-round', { roomNumber: next.roomNumber });
                }
            }));
            actions.appendChild(button('解散房间', 'btn-danger', true, function () {
                if (window.confirm('解散后这副牌就作废了。')) send('/app/dual/close', { roomNumber: next.roomNumber });
            }));
            return;
        }
        if (!next.dealt) {
            actions.appendChild(button(next.ready ? '取消准备' : '准备', next.ready ? 'btn-secondary' : 'btn-primary', !!next.mySeat, function () {
                send('/app/dual/ready', { roomNumber: next.roomNumber, ready: !next.ready });
            }));
            actions.appendChild(button('离开房间', 'btn-danger', true, leaveRoom));
            return;
        }
        actions.appendChild(button(cardHidden ? '查看身份' : '遮住身份', 'btn-primary', true, function () {
            cardHidden = !cardHidden;
            paintHand(state);
            paintActions(state);
        }));
        actions.appendChild(button('离开房间', 'btn-secondary', true, leaveRoom));
    }

    function paintConfig(next) {
        var box = document.getElementById('role-config');
        if (!next.host || next.dealt || !next.config) {
            box.style.display = 'none';
            return;
        }
        box.style.display = 'block';
        box.innerHTML = '';
        var counts = document.createElement('div');
        counts.className = 'count-row';
        [6, 7, 8].forEach(function (count) {
            counts.appendChild(button(count + '人', next.config.players === count ? 'btn-primary' : 'btn-secondary', true, function () {
                sendConfig(count);
            }));
        });
        box.appendChild(counts);
        var text = document.createElement('div');
        text.style.marginTop = '8px';
        text.textContent = next.configText;
        box.appendChild(text);
    }

    function sendConfig(players) {
        send('/app/dual/config', {
            roomNumber: state.roomNumber,
            players: players
        });
    }

    function leaveRoom() {
        send('/app/dual/leave', { roomNumber: state ? state.roomNumber : localStorage.getItem('dualRoom') });
    }

    function button(text, className, enabled, onClick) {
        var node = document.createElement('button');
        node.type = 'button';
        node.className = 'btn ' + className;
        node.textContent = text;
        node.disabled = !enabled;
        node.onclick = onClick;
        return node;
    }

    function reconcileJudgeMarks(previous, next) {
        if (!next.host) {
            return;
        }
        var deadKey = deadStorageKey(next);
        var sigKey = 'dualDealSig:' + next.roomNumber + ':' + next.round;
        if (!next.dealt) {
            localStorage.removeItem(deadKey);
            localStorage.removeItem(sigKey);
            return;
        }
        var sig = (next.seats || []).map(function (info) {
            return info.seat + '=' + (info.cardAName || '') + '/' + (info.cardBName || '');
        }).join(',');
        var redeal = previous && (!previous.dealt || previous.round !== next.round || previous.roomNumber !== next.roomNumber);
        if (redeal || localStorage.getItem(sigKey) !== sig) {
            localStorage.removeItem(deadKey);
            localStorage.setItem(sigKey, sig);
        }
    }

    function deadStorageKey(next) {
        return 'dualDead:' + next.roomNumber + ':' + next.round;
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

    function imageSrc(url) {
        if (url && url.indexOf('img/lrs/') >= 0 && url.slice(-4) === '.png') {
            url = url.slice(0, -4) + '.jpg';
        }
        var slash = url.lastIndexOf('/');
        if (slash < 0) return encodeURIComponent(url);
        return url.slice(0, slash + 1) + encodeURIComponent(url.slice(slash + 1)) + '?v=s2';
    }

    function show(id) {
        document.getElementById('page-home').classList.remove('active');
        document.getElementById('page-room').classList.remove('active');
        document.getElementById(id).classList.add('active');
    }

    function showError(id, message) {
        var node = document.getElementById(id);
        node.textContent = message || '';
        node.style.display = message ? 'block' : 'none';
    }
})();
