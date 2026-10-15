let gameState = {};
const roleNames = {
    civilian: '平民',
    werewolf: '狼人',
    wolfKing: '狼王',
    seer: '预言家',
    witch: '女巫',
    hunter: '猎人',
    guard: '守卫'
};
const roleAvatars = {civilian: '🧑‍🌾', werewolf: '🐺', wolfKing: '👑', seer: '🔮', witch: '🧙‍♀️', hunter: '🏹', guard: '🛡️'};

// ---- 初始化与配置 ----
function resetGameState() {
    gameState = {
        config: {
            civilian: 3,
            werewolf: 2,
            wolfKing: true,
            seer: true,
            witch: true,
            hunter: true,
            guard: true,
            witchCanSaveSelfN1: true
        },
        players: [],
        playerIdentities: {},
        isFirstNight: true,
        nightCount: 1,
        phaseOrder: [],
        currentPhaseIndex: -1,
        nightActions: {},
        witchAntidoteAvailable: true,
        witchPoisonAvailable: true,
        hunterCanShoot: true,
        deathQueue: [],
        tempSelections: {identity: null, action: null, wolves: []}
    };
}

function adjustRole(role, delta) {
    const el = document.getElementById(role + 'Count');
    let val = parseInt(el.textContent);
    if (val + delta >= 0) {
        el.textContent = val + delta;
        updateTotalCount();
    }
}

function toggleRole(role) {
    document.getElementById(role + 'Toggle').classList.toggle('active');
    updateTotalCount();
}

function toggleWitchSelfSave() {
    document.getElementById('witchSelfSaveToggle').classList.toggle('active');
}

function updateTotalCount() {
    const cfg = {
        civilian: parseInt(document.getElementById('civilianCount').textContent),
        werewolf: parseInt(document.getElementById('werewolfCount').textContent),
        wolfKing: document.getElementById('wolfKingToggle').classList.contains('active'),
        seer: document.getElementById('seerToggle').classList.contains('active'),
        witch: document.getElementById('witchToggle').classList.contains('active'),
        hunter: document.getElementById('hunterToggle').classList.contains('active'),
        guard: document.getElementById('guardToggle').classList.contains('active'),
        witchCanSaveSelfN1: document.getElementById('witchSelfSaveToggle').classList.contains('active')
    };
    cfg.total = cfg.civilian + cfg.werewolf + (cfg.wolfKing ? 1 : 0) + (cfg.seer ? 1 : 0) + (cfg.witch ? 1 : 0) + (cfg.hunter ? 1 : 0) + (cfg.guard ? 1 : 0);
    gameState.config = cfg;
    document.getElementById('totalCount').textContent = cfg.total;
}

function showSetupPhase() {
    document.getElementById('welcomePhase').classList.remove('active');
    document.getElementById('setupPhase').classList.add('active');
}

function confirmSetup() {
    updateTotalCount();
    if (gameState.config.total < 6) {
        showAlert('总人数不能少于6人！');
        return;
    }
    for (let i = 1; i <= gameState.config.total; i++) {
        gameState.players.push({number: i, alive: true});
    }
    document.getElementById('setupPhase').classList.remove('active');
    document.getElementById('gamePhase').classList.add('active');
    addLog(`游戏配置完毕，共 ${gameState.config.total} 人。`);
    startNight();
}

// ---- 游戏主流程与导航 ----
function backToHome() {
    showConfirm('确定要返回首页吗？<br>所有游戏进度将丢失。', () => {
        clearLog();
        document.getElementById('gamePhase').classList.remove('active');
        document.getElementById('setupPhase').classList.remove('active');
        document.getElementById('welcomePhase').classList.add('active');
        resetGameState();
        updateTotalCount();
        addLog("返回首页，游戏已重置。");
    });
}

function resetCurrentGame() {
    showConfirm('确定要重置当前游戏吗？<br>将返回第一夜。', () => {
        clearLog();
        const savedConfig = {...gameState.config};
        resetGameState();
        gameState.config = savedConfig;
        for (let i = 1; i <= gameState.config.total; i++) {
            gameState.players.push({number: i, alive: true});
        }
        addLog(`游戏已重置，配置保留，返回第一夜。`);
        startNight();
    });
}

function startNight() {
    hideAllActionCards();
    gameState.nightActions = {
        lastGuardTarget: gameState.nightActions?.guardTarget || null,
        witchUsedAntidote: false,
        witchUsedPoison: false,
        witchPoisonTarget: null
    };
    gameState.deathQueue = [];
    gameState.tempSelections = {identity: null, action: null, wolves: []};
    document.getElementById('phaseIndicator').textContent = `第${gameState.nightCount}夜 🌙 天黑请闭眼`;
    const order = [];
    const cfg = gameState.config;
    if (cfg.guard) order.push('guard');
    if (cfg.werewolf > 0 || cfg.wolfKing) order.push('werewolf');
    if (cfg.witch) order.push('witch');
    if (cfg.seer) order.push('seer');
    if (cfg.hunter) order.push('hunter');
    gameState.phaseOrder = order;
    gameState.currentPhaseIndex = -1;
    nextPhase();
}

function previousPhase() {
    if (gameState.currentPhaseIndex <= 0) return;

    // 获取我们即将离开的阶段（例如，女巫）
    const leavingRole = gameState.phaseOrder[gameState.currentPhaseIndex];
    const beforeRole = gameState.phaseOrder[gameState.currentPhaseIndex-1];
    // --- 核心逻辑：彻底回滚上一个阶段的状态 ---

    if (gameState.isFirstNight) {
        // 在第一夜，回退意味着要清除身份和行动记录

        // 1. 移除日志
        // 狼人阶段有2条日志(团队指认+刀人), 其他神职有2条(指认+行动), 所以统一移除2条
        const log = document.getElementById('gameLog');
        if (log.firstChild) log.removeChild(log.firstChild);
        if (log.firstChild) log.removeChild(log.firstChild);

        // 2. 清除已记录的身份
        if (beforeRole === 'werewolf') {
            // 如果离开的是狼人阶段，则清除所有狼人和狼王的身份
            Object.keys(gameState.playerIdentities).forEach(pNum => {
                if (['werewolf', 'wolfKing'].includes(gameState.playerIdentities[pNum])) {
                    delete gameState.playerIdentities[pNum];
                }
            });
        } else {
            // 如果离开的是其他神职阶段，则清除该神职的身份
            const ownerId = findRoleOwner(beforeRole);
            if (ownerId) {
                delete gameState.playerIdentities[ownerId];
            }
        }
    } else {
        // 在后续夜晚，身份是固定的，我们只需要清除上一部的行动日志
        const log = document.getElementById('gameLog');
        if (log.firstChild) log.removeChild(log.firstChild);
    }

    // 3. 清除已记录的行动 (对所有夜晚都适用)
    switch(leavingRole) {
        case 'werewolf': gameState.nightActions.werewolfTarget = null; break;
        case 'guard': gameState.nightActions.guardTarget = null; break;
        case 'seer': gameState.nightActions.seerTarget = null; break;
        case 'witch':
            // 重置女巫的所有可能操作
            gameState.nightActions.witchUsedAntidote = false;
            gameState.nightActions.witchUsedPoison = false;
            gameState.nightActions.witchPoisonTarget = null;
            break;
    }

    // --- 导航到上一步 ---
    gameState.currentPhaseIndex--;
    showPhase(gameState.phaseOrder[gameState.currentPhaseIndex]);
}

function nextPhase() {
    gameState.currentPhaseIndex++;
    if (gameState.currentPhaseIndex < gameState.phaseOrder.length) {
        showPhase(gameState.phaseOrder[gameState.currentPhaseIndex]);
    } else {
        processNightResults();
    }
}

function showPhase(role) {
    hideAllActionCards();
    gameState.tempSelections = {identity: null, action: null, wolves: []};
    const card = document.getElementById('actionCard');
    const title = document.getElementById('actionCardTitle');
    const content = document.getElementById('actionCardContent');
    card.style.display = 'block';
    const roleFullName = `${roleAvatars[role]} ${roleNames[role]}`;
    title.textContent = `${roleFullName}阶段`;
    document.getElementById('phaseIndicator').textContent = `第${gameState.nightCount}夜 · ${roleFullName}阶段`;
    const isIdentityKnown = !!findRoleOwner(role);
    if (gameState.isFirstNight && !isIdentityKnown) {
        switch (role) {
            case 'werewolf':
                content.innerHTML = buildN1WerewolfUI();
                break;
            case 'witch':
                content.innerHTML = buildN1WitchUI();
                break;
            default:
                content.innerHTML = buildN1SingleRoleUI(role);
                break;
        }
    } else {
        const roleOwnerId = findRoleOwner(role);
        const areWolvesAlive = Object.keys(gameState.playerIdentities).some(p => ['werewolf', 'wolfKing'].includes(gameState.playerIdentities[p]) && isPlayerAlive(p));
        if ((role === 'werewolf' && areWolvesAlive) || (role !== 'werewolf' && roleOwnerId && isPlayerAlive(roleOwnerId))) {
            switch (role) {
                case 'werewolf':
                    content.innerHTML = buildWerewolfActionUI();
                    break;
                case 'witch':
                    content.innerHTML = buildWitchActionUI(roleOwnerId);
                    break;
                case 'hunter':
                    content.innerHTML = buildHunterStatusUI();
                    break;
                default:
                    content.innerHTML = buildSingleRoleActionUI(role, roleOwnerId);
                    break;
            }
        } else {
            content.innerHTML = buildDeadRoleUI(role);
        }
    }
}

// ---- UI 构建函数 ----
function buildActionButtons(confirmFn, confirmText = '确认') {
    let backBtn = (gameState.currentPhaseIndex > 0) ? `<button class="btn btn-secondary" onclick="previousPhase()">上一步</button>` : '';
    return `<div class="action-buttons">${backBtn}<button class="btn btn-primary" onclick="${confirmFn}">${confirmText}</button></div>`;
}

function buildN1SingleRoleUI(role) {
    const isHunter = role === 'hunter';
    let html = `<div><h4>1. 指认身份</h4><div class="prompt-box">${roleNames[role]}请睁眼，（法官请标记身份）。</div><div class="player-selector" id="identitySelector"></div></div>`;
    if (!isHunter) {
        html += `<hr><div id="actionSection" class="action-section"><h4>2. 执行行动</h4><div class="prompt-box" id="actionPrompt">请先在上方指认身份。</div><div class="player-selector" id="actionSelector"></div><div id="seerResult" style="margin-top: 15px; display: none;"><div class="prompt-box" id="seerResultText"></div></div></div>`;
    }
    html += buildActionButtons(`confirmN1SingleRole('${role}')`);
    setTimeout(() => {
        populatePlayerSelector('identitySelector', handleIdentitySelection, () => true, (p) => !!gameState.playerIdentities[p.number]);
        if (!isHunter) document.getElementById('actionSelector').innerHTML = '<p style="text-align: center; opacity: 0.6;">等待指认身份...</p>';
    }, 0);
    return html;
}

function buildN1WerewolfUI() {
    const expectedCount = gameState.config.werewolf + (gameState.config.wolfKing ? 1 : 0);
    let html = `<div><h4>1. 指认狼人团队 (共${expectedCount}人)</h4><div class="prompt-box">狼人请睁眼，（法官请标记所有狼人）。</div><div class="player-selector" id="identitySelector"></div></div><div id="wolfKingSection" style="display:none;"><hr><h4>2. 指认狼王</h4><div class="prompt-box">狼王请给我一个手势，（法官请标记狼王）。</div><div class="player-selector" id="wolfKingSelector"></div></div><hr><div id="actionSection" class="action-section"><h4>${gameState.config.wolfKing ? '3.' : '2.'} 执行行动</h4><div class="prompt-box" id="actionPrompt">请统一意见，选择今晚要刀杀的玩家。</div><div class="player-selector" id="actionSelector"></div></div>`;
    html += buildActionButtons('confirmN1Werewolf()');
    setTimeout(() => {
        populatePlayerSelector('identitySelector', handleMultiIdentitySelection, () => true, (p) => !!gameState.playerIdentities[p.number]);
        document.getElementById('actionSelector').innerHTML = '<p style="text-align: center; opacity: 0.6;">等待指认身份...</p>';
    }, 0);
    return html;
}

function buildN1WitchUI() {
    let html = `<div><h4>1. 指认身份</h4><div class="prompt-box">女巫请睁眼，（法官请标记身份）。</div><div class="player-selector" id="identitySelector"></div></div><hr><div id="actionSection" class="action-section"><h4>2. 执行行动</h4>${buildWitchActionContent()}</div>`;
    html += buildActionButtons('confirmN1Witch()');
    setTimeout(() => {
        populatePlayerSelector('identitySelector', handleIdentitySelection, () => true, (p) => !!gameState.playerIdentities[p.number]);
    }, 0);
    return html;
}

function buildSingleRoleActionUI(role, roleId) {
    const prompts = {guard: '守卫请睁眼，请选择你要守护的玩家。', seer: '预言家请睁眼，请选择你要查验的玩家。'};
    let html = `<div class="prompt-box">${prompts[role]}</div><div class="player-selector" id="actionSelector"></div><div id="seerResult" style="margin-top: 15px; display: none;"><div class="prompt-box" id="seerResultText"></div></div>`;
    html += buildActionButtons(`confirmAction('${role}')`);
    setTimeout(() => {
        populatePlayerSelector('actionSelector', handleActionSelection, () => true, (p) => role === 'guard' && p.number === gameState.nightActions.lastGuardTarget);
    }, 0);
    return html;
}

function buildWerewolfActionUI() {
    let html = `<div class="prompt-box">狼人请睁眼，请统一意见，选择今晚要刀杀的玩家。</div><div class="player-selector" id="actionSelector"></div>`;
    html += buildActionButtons(`confirmAction('werewolf')`);
    setTimeout(() => {
        populatePlayerSelector('actionSelector', handleActionSelection);
    }, 0);
    return html;
}

function buildWitchActionUI(witchNumber) {
    let html = `<div class="prompt-box">女巫请睁眼。</div>${buildWitchActionContent()}`;
    html += buildActionButtons(`confirmAction('witch')`);
    setTimeout(() => {
        updateWitchUI(witchNumber);
    }, 0);
    return html;
}

function buildWitchActionContent() {
    return `<div class="prompt-box" id="witchVictimPrompt"></div><div class="witch-choice"><div class="choice-box antidote" id="antidoteChoice" onclick="useAntidote()"><h4 style="color:#000">💊 解药</h4><p id="antidoteStatus"></p></div><div class="choice-box poison" id="poisonChoice" onclick="usePoison()"><h4 style="color:#000">☠️ 毒药</h4><p id="poisonStatus"></p></div></div><div id="poisonSelectorContainer" style="display:none;"><h4>请选择要毒杀的玩家：</h4><div class="player-selector" id="poisonSelector"></div></div>`;
}

function buildDeadRoleUI(role) {
    const prompts = {
        guard: '守卫请睁眼。',
        seer: '预言家请睁眼。',
        witch: '女巫请睁眼。',
        werewolf: '狼人请睁眼。',
        hunter: '猎人请睁眼。'
    };
    const roleFullName = `${roleAvatars[role]} ${roleNames[role]}`;
    const text = `${prompts[role]}<br><br><b style="color:#fa709a">(${roleFullName} 已阵亡，无法行动)</b>`;
    return `<div class="prompt-box">${text}</div>${buildActionButtons('nextPhase()', '继续')}`;
}

function getNightActionSummary() {
    const nightActions = gameState.nightActions;

    // 女巫的解药总是用于狼人的目标
    const antidoteTarget = nightActions.witchUsedAntidote ? nightActions.werewolfTarget : null;

    return {
        werewolfTarget: nightActions.werewolfTarget || null, // 狼人目标
        guardTarget: nightActions.guardTarget || null,       // 守卫目标
        antidoteTarget: antidoteTarget,                      // 解药目标
        poisonTarget: nightActions.witchPoisonTarget || null // 毒药目标
    };
}

/**
 * @description 检查猎人今晚是否被狼人成功击杀（综合守卫和女巫的情况）
 * @returns {boolean} 如果猎人被狼人成功击杀，则返回 true，否则返回 false。
 */
function checkIfHunterDiesToWolvesTonight() {
    // 1. 找到场上的猎人是谁
    const hunterId = findRoleOwner('hunter');
    // 如果没有猎人，或者猎人已经死亡，则直接返回 false
    if (!hunterId || !isPlayerAlive(hunterId)) {
        return false;
    }

    // 2. 获取当晚的所有行动总结
    const summary = getNightActionSummary();

    // 3. 开始执行您的判断逻辑

    // 如果狼人刀的人不是猎人，那猎人肯定安全
    if (summary.werewolfTarget !== hunterId) {
        return false;
    }

    // 如果狼人刀的是猎人，再判断是否被成功守护或拯救
    if (summary.guardTarget === hunterId || summary.antidoteTarget === hunterId) {
        // 守卫守护了猎人，或者女巫拯救了猎人，则猎人安全
        return false;
    }

    // 如果狼人刀了猎人，且猎人既没被守护也没被拯救，那么他就会死亡
    return true;
}

function buildHunterStatusUI() {
    let skillStatusText = '';
    if (checkIfHunterDiesToWolvesTonight()) {
        skillStatusText = `
            <div style="font-size: 16px; margin-bottom: 8px;">
                 <b style="color:#84fab0;">技能准备就绪 ✅</b>
            </div>`;
    } else {
        skillStatusText = `
            <div style="font-size: 16px; margin-bottom: 8px;">
                状态: <b style="color:#fa709a;">技能不可用 ❌</b>
            </div>`;
    }
    const text = `猎人请睁眼，你的开枪状态是：<br>${skillStatusText}`;
    return `<div class="prompt-box" style="text-align: left; padding: 15px;">${text}</div>${buildActionButtons('nextPhase()', '继续')}`;
}

// ---- 夜晚操作处理 ----
function updateSelectionUI(selectorId, currentSelection) {
    document.querySelectorAll(`#${selectorId} .player-number`).forEach(div => {
        div.classList.toggle('selected', div.dataset.number == currentSelection);
    });
}

function updateMultiSelectionUI(selectorId, selections) {
    document.querySelectorAll(`#${selectorId} .player-number`).forEach(div => {
        div.classList.toggle('selected', selections.includes(parseInt(div.dataset.number)));
    });
}

function handleIdentitySelection(playerNumber) {
    gameState.tempSelections.identity = playerNumber;
    updateSelectionUI('identitySelector', playerNumber);
    const actionSection = document.getElementById('actionSection');
    if (actionSection) actionSection.classList.add('enabled');
    const titleText = document.getElementById('actionCardTitle').textContent;
    const roleKey = Object.keys(roleAvatars).find(key => titleText.includes(roleAvatars[key]));
    if (roleKey === 'witch') {
        updateWitchUI(playerNumber);
    } else if (actionSection) {
        document.getElementById('actionPrompt').textContent = `请选择你要${roleKey === 'seer' ? '查验' : '守护'}的玩家号码。`;
        populatePlayerSelector('actionSelector', handleActionSelection, () => true, (p) => roleKey === 'guard' && p.number === gameState.nightActions.lastGuardTarget);
    }
}

function handleMultiIdentitySelection(playerNumber) {
    const {wolves} = gameState.tempSelections;
    const index = wolves.indexOf(playerNumber);
    if (index > -1) wolves.splice(index, 1); else wolves.push(playerNumber);
    updateMultiSelectionUI('identitySelector', wolves);
    const expectedCount = gameState.config.werewolf + (gameState.config.wolfKing ? 1 : 0);
    const actionSection = document.getElementById('actionSection');
    const wolfKingSection = document.getElementById('wolfKingSection');
    if (wolves.length === expectedCount) {
        actionSection.classList.add('enabled');
        populatePlayerSelector('actionSelector', handleActionSelection);
        if (gameState.config.wolfKing) {
            wolfKingSection.style.display = 'block';
            const selector = document.getElementById('wolfKingSelector');
            selector.innerHTML = '';
            wolves.forEach(num => {
                const div = createPlayerNumber(num);
                div.onclick = () => {
                    gameState.tempSelections.identity = num;
                    updateSelectionUI('wolfKingSelector', num);
                };
                selector.appendChild(div);
            });
        }
    } else {
        actionSection.classList.remove('enabled');
        if (wolfKingSection) wolfKingSection.style.display = 'none';
        document.getElementById('actionSelector').innerHTML = '<p style="text-align: center; opacity: 0.6;">等待指认身份...</p>';
    }
}

function handleActionSelection(playerNumber) {
    if (gameState.tempSelections.action === playerNumber) {
        gameState.tempSelections.action = null;
    } else {
        gameState.tempSelections.action = playerNumber;
    }
    updateSelectionUI('actionSelector', gameState.tempSelections.action);
    const titleText = document.getElementById('actionCardTitle').textContent;
    const roleKey = Object.keys(roleAvatars).find(key => titleText.includes(roleAvatars[key]));
    if (roleKey === 'seer') {
        showSeerCheckResult(playerNumber);
    }
}

function confirmN1Witch() {
    const {identity} = gameState.tempSelections;
    if (!identity) {
        showAlert(`请先指认女巫！`);
        return;
    }
    confirmAction('witch', identity);
}

function confirmN1SingleRole(role) {
    const {identity, action} = gameState.tempSelections;
    if (!identity) {
        showAlert(`请先指认${roleNames[role]}！`);
        return;
    }
    gameState.playerIdentities[identity] = role;
    addLog(`第一夜：指认 ${identity}号 为 ${roleNames[role]}`);
    if (role === 'hunter') {
        document.getElementById('actionCardContent').innerHTML = buildHunterStatusUI();
        return;
    }
    if (!action && role !== 'guard') {
        showAlert('请选择行动目标！');
        return;
    }
    if (role === 'guard') {
        gameState.nightActions.guardTarget = action;
        addLog(`第一夜：守卫(${identity}号)守护了 ${action || '无人'}`);
    }
    if (role === 'seer') {
        gameState.nightActions.seerTarget = action;
        addLog(`第一夜：预言家(${identity}号)查验了 ${action}号`);
    }
    nextPhase();
}

function confirmN1Werewolf() {
    const {wolves, identity, action} = gameState.tempSelections;
    const expectedCount = gameState.config.werewolf + (gameState.config.wolfKing ? 1 : 0);
    if (wolves.length !== expectedCount) {
        showAlert(`狼人数量错误！应为${expectedCount}人。`);
        return;
    }
    if (gameState.config.wolfKing && !identity) {
        showAlert('请指认狼王！');
        return;
    }
    wolves.forEach(num => gameState.playerIdentities[num] = 'werewolf');
    if (gameState.config.wolfKing) gameState.playerIdentities[identity] = 'wolfKing';
    gameState.nightActions.werewolfTarget = action;
    addLog(`第一夜：指认 ${wolves.join('、')}号 为狼人团队` + (gameState.config.wolfKing ? `, 其中 ${identity}号 为狼王。` : '。'));
    addLog(`第一夜：狼人团队刀杀了 ${action || '无人（空刀）'}号。`);
    nextPhase();
}

function confirmAction(role, n1Identity) {
    const roleId = getPlayerByRole(role) || n1Identity;
    if (!roleId && role !== 'werewolf') return;
    if (role === 'werewolf') {
        gameState.nightActions.werewolfTarget = gameState.tempSelections.action;
        addLog(`第${gameState.nightCount}夜：狼人团队刀杀了 ${gameState.tempSelections.action || '无人（空刀）'}号。`);
    } else if (role === 'seer') {
        if (!gameState.tempSelections.action) {
            showAlert('请选择查验目标！');
            return;
        }
        gameState.nightActions.seerTarget = gameState.tempSelections.action;
        addLog(`第${gameState.nightCount}夜：${roleNames[role]}(${roleId}号)对 ${gameState.tempSelections.action}号 执行了行动。`);
    } else if (role === 'guard') {
        gameState.nightActions.guardTarget = gameState.tempSelections.action;
        addLog(`第${gameState.nightCount}夜：${roleNames[role]}(${roleId}号)对 ${gameState.tempSelections.action || '无人'}号 执行了行动。`);
    } else if (role === 'witch') {
        if (n1Identity) gameState.playerIdentities[n1Identity] = 'witch';
        const currentWitchId = roleId || n1Identity;
        if (gameState.nightActions.witchUsedPoison && !gameState.nightActions.witchPoisonTarget) {
            showAlert('请选择一个毒杀目标！');
            return;
        }
        if (gameState.nightActions.witchUsedAntidote) gameState.witchAntidoteAvailable = false;
        if (gameState.nightActions.witchUsedPoison) gameState.witchPoisonAvailable = false;
        if (gameState.nightActions.witchUsedAntidote || gameState.nightActions.witchUsedPoison) addLog(`第${gameState.nightCount}夜：女巫(${currentWitchId}号)使用了药物。`);
    }
    nextPhase();
}

function showSeerCheckResult(targetNumber) {
    const role = gameState.playerIdentities[targetNumber] || 'civilian';
    const isWolf = (role === 'werewolf' || role === 'wolfKing');
    const resultText = isWolf ? '🐺 狼人' : '🧑‍🌾 好人';
    const resultBox = document.getElementById('seerResult');
    if (resultBox) {
        resultBox.style.display = 'block';
        document.getElementById('seerResultText').innerHTML = `查验结果：<b>${targetNumber}号</b> 玩家的身份是【<b>${resultText}</b>】`;
    }
}

function updateWitchUI(witchNumber) {
    const target = gameState.nightActions.werewolfTarget;
    const isGuarded = (target === gameState.nightActions.guardTarget);
    const witchIsTarget = (witchNumber === target);
    let promptText = '';
    if (gameState.witchAntidoteAvailable) {
        let extraInfo = '';
        if (target && gameState.playerIdentities[target] === 'hunter') {
            extraInfo = `<br><small style="color:#84fab0;">（若不使用解药，此玩家为猎人，天亮时可以开枪）</small>`;
        }
        promptText = target ? `今晚 ${target}号 玩家倒牌了，你要使用解药吗？${extraInfo}` : `今晚是平安夜。`;
    } else {
        promptText = `你的解药已使用，你要使用毒药吗？`;
    }
    document.getElementById('witchVictimPrompt').innerHTML = promptText;
    const antidoteBox = document.getElementById('antidoteChoice');
    document.getElementById('antidoteStatus').textContent = gameState.witchAntidoteAvailable ? '未使用' : '已用';
    let disableAntidote = !gameState.witchAntidoteAvailable || isGuarded || !target;
    if (witchIsTarget) {
        if (gameState.isFirstNight) {
            if (!gameState.config.witchCanSaveSelfN1) disableAntidote = true;
        } else {
            disableAntidote = true;
        }
    }
    antidoteBox.classList.toggle('disabled', disableAntidote);
    antidoteBox.classList.toggle('selected', gameState.nightActions.witchUsedAntidote);
    const poisonBox = document.getElementById('poisonChoice');
    document.getElementById('poisonStatus').textContent = gameState.witchPoisonAvailable ? '未使用' : '已用';
    const disablePoison = !gameState.witchPoisonAvailable;
    poisonBox.classList.toggle('disabled', disablePoison);
    poisonBox.classList.toggle('selected', gameState.nightActions.witchUsedPoison);
    if (gameState.nightActions.witchUsedPoison && !disablePoison) {
        document.getElementById('poisonSelectorContainer').style.display = 'block';
        populatePlayerSelector('poisonSelector', (num) => {
            document.getElementById('poisonSelector').querySelector('.selected')?.classList.remove('selected');
            document.querySelector(`#poisonSelector .player-number[data-number='${num}']`).classList.add('selected');
            gameState.nightActions.witchPoisonTarget = num;
        });
    } else {
        document.getElementById('poisonSelectorContainer').style.display = 'none';
    }
}

function useAntidote() {
    const box = document.getElementById('antidoteChoice');
    if (box.classList.contains('disabled')) return;
    if (gameState.nightActions.witchUsedPoison) {
        gameState.nightActions.witchUsedPoison = false;
        document.getElementById('poisonChoice').classList.remove('selected');
    }
    gameState.nightActions.witchUsedAntidote = !gameState.nightActions.witchUsedAntidote;
    box.classList.toggle('selected', gameState.nightActions.witchUsedAntidote);
    updateWitchUI(getPlayerByRole('witch') || gameState.tempSelections.identity);
}

function usePoison() {
    const box = document.getElementById('poisonChoice');
    if (box.classList.contains('disabled')) return;
    if (gameState.nightActions.witchUsedAntidote) {
        gameState.nightActions.witchUsedAntidote = false;
        document.getElementById('antidoteChoice').classList.remove('selected');
    }
    gameState.nightActions.witchUsedPoison = !gameState.nightActions.witchUsedPoison;
    box.classList.toggle('selected', gameState.nightActions.witchUsedPoison);
    updateWitchUI(getPlayerByRole('witch') || gameState.tempSelections.identity);
}

// ---- 天亮、投票与技能处理 ----
function processNightResults() {
    hideAllActionCards();
    const dayPhase = document.getElementById('dayPhase');
    dayPhase.style.display = 'block';
    document.getElementById('phaseIndicator').textContent = `☀️ 第${gameState.nightCount}天`;
    if (gameState.isFirstNight) {
        finalizeIdentities();
    }
    const {werewolfTarget, guardTarget, witchUsedAntidote, witchPoisonTarget} = gameState.nightActions;
    if (werewolfTarget) {
        if (werewolfTarget === guardTarget && witchUsedAntidote) {
            gameState.witchAntidoteAvailable = true;
            addLog("触发同守同救，女巫解药返还。");
        }
        if (werewolfTarget !== guardTarget && !witchUsedAntidote) {
            killPlayer(werewolfTarget, 'killed');
        }
    }
    if (witchPoisonTarget) {
        killPlayer(witchPoisonTarget, 'poisoned');
    }
    if (gameState.deathQueue.length > 0) {
        const deadText = gameState.deathQueue.map(p => `${p.number}号`).join(' 和 ');
        document.getElementById('dayResult').textContent = `昨晚 ${deadText} 玩家死亡。`;
    } else {
        document.getElementById('dayResult').textContent = '昨晚是平安夜，无人死亡。';
    }
    gameState.isFirstNight = false;
    updateIdentitySummary();
    document.getElementById('identitySummaryContainer').style.display = 'block';
    processDeathQueue(() => showVotingButton());
}

function showVotingPhase() {
    hideAllActionCards();
    const votingPhase = document.getElementById('votingPhase');
    votingPhase.style.display = 'block';
    document.getElementById('phaseIndicator').textContent = `🏙️ 第${gameState.nightCount}天 白天`;
    populatePlayerSelector('voteSelector', (num) => {
        document.querySelector('#voteSelector .selected')?.classList.remove('selected');
        document.querySelector(`#voteSelector .player-number[data-number='${num}']`).classList.add('selected');
    });
}

function confirmVote() {
    const selected = document.querySelector('#voteSelector .player-number.selected');
    if (!selected) {
        showAlert('请选择要放逐的玩家！');
        return;
    }
    const votedPlayerNumber = parseInt(selected.dataset.number);
    const role = gameState.playerIdentities[votedPlayerNumber];
    addLog(`第${gameState.nightCount}天：${votedPlayerNumber}号(${roleNames[role]}) 被投票放逐。`);
    hideAllActionCards();
    const dayPhase = document.getElementById('dayPhase');
    dayPhase.style.display = 'block';
    dayPhase.querySelector('.prompt-box').textContent = `${votedPlayerNumber}号 玩家被放逐，其身份是 ${roleAvatars[role]} ${roleNames[role]}。`;
    dayPhase.querySelector('.action-buttons').innerHTML = '';
    killPlayer(votedPlayerNumber, 'voted');
    processDeathQueue(() => showNextNightButton());
}

function processDeathQueue(continuationFn) {
    updateIdentitySummary();
    if (gameState.deathQueue.length === 0) {
        const winner = checkWinConditions();
        if (winner) {
            announceWinnerAndEndGame(winner);
        } else {
            continuationFn();
        }
        return;
    }
    const death = gameState.deathQueue.shift();
    const {number, reason} = death;
    const role = gameState.playerIdentities[number];
    let canUseSkill = (role === 'hunter' && reason !== 'poisoned' && gameState.hunterCanShoot) || (role === 'wolfKing' && reason !== 'poisoned');
    if (canUseSkill) {
        showConfirm(`${roleNames[role]} (${number}号) 已死亡，是否发动技能？`, () => showSkillPhase(number, role, continuationFn), () => {
            if (role === 'hunter') gameState.hunterCanShoot = false;
            addLog(`${roleNames[role]}放弃发动技能。`);
            processDeathQueue(continuationFn);
        });
    } else {
        processDeathQueue(continuationFn);
    }
}

function showSkillPhase(playerNumber, role, continuationFn) {
    hideAllActionCards();
    const skillPhase = document.getElementById('skillPhase');
    skillPhase.style.display = 'block';
    const roleFullName = `${roleAvatars[role]} ${roleNames[role]}`;
    document.getElementById('skillTitle').textContent = `${roleFullName}发动技能`;
    document.getElementById('skillPrompt').textContent = `${playerNumber}号 ${roleFullName} 请选择一名玩家带走。`;
    populatePlayerSelector('skillSelector', (num) => {
        gameState.tempSelections.action = num;
        updateSelectionUI('skillSelector', num);
    });
    const buttonsContainer = document.getElementById('skillActionButtons');
    buttonsContainer.innerHTML = `<button class="btn btn-secondary" id="skill-skip-btn">放弃技能</button><button class="btn btn-danger" id="skill-confirm-btn">确认带走</button>`;
    document.getElementById('skill-skip-btn').onclick = () => confirmSkillAction(role, null, continuationFn);
    document.getElementById('skill-confirm-btn').onclick = () => {
        const target = gameState.tempSelections.action;
        if (!target) {
            showAlert('请选择一个目标，或点击“放弃技能”。');
            return;
        }
        confirmSkillAction(role, target, continuationFn);
    };
}

function confirmSkillAction(role, targetNumber, continuationFn) {
    hideAllActionCards();
    if (targetNumber) {
        killPlayer(targetNumber, 'skilled');
        addLog(`${roleNames[role]}发动技能带走了 ${targetNumber}号。`);
    } else {
        addLog(`${roleNames[role]}放弃发动技能。`);
    }
    if (role === 'hunter') gameState.hunterCanShoot = false;
    processDeathQueue(continuationFn);
}

function showNextNightButton() {
    hideAllActionCards();
    const dayPhase = document.getElementById('dayPhase');
    dayPhase.style.display = 'block';
    const btn = document.createElement('button');
    btn.className = 'btn btn-primary';
    btn.textContent = '进入下一夜';
    btn.onclick = () => {
        gameState.nightCount++;
        startNight();
    };
    document.getElementById('dayActionButtons').innerHTML = '';
    document.getElementById('dayActionButtons').appendChild(btn);
}

function showVotingButton() {
    hideAllActionCards();
    const dayPhase = document.getElementById('dayPhase');
    dayPhase.style.display = 'block';
    const btn = document.createElement('button');
    btn.className = 'btn btn-primary';
    btn.textContent = '进入投票';
    btn.onclick = showVotingPhase;
    document.getElementById('dayActionButtons').innerHTML = '';
    document.getElementById('dayActionButtons').appendChild(btn);
}

// ---- 辅助与模态框函数 ----
function checkWinConditions() {
    const wolves = Object.keys(gameState.playerIdentities).filter(p => ['werewolf', 'wolfKing'].includes(gameState.playerIdentities[p]));
    const gods = Object.keys(gameState.playerIdentities).filter(p => ['seer', 'witch', 'hunter', 'guard'].includes(gameState.playerIdentities[p]));
    const villagers = Object.keys(gameState.playerIdentities).filter(p => gameState.playerIdentities[p] === 'civilian');
    const allWolvesDead = wolves.length > 0 && wolves.every(p => !isPlayerAlive(p));
    const allGodsDead = gods.length > 0 && gods.every(p => !isPlayerAlive(p));
    const allVillagersDead = villagers.length > 0 && villagers.every(p => !isPlayerAlive(p));
    if (allWolvesDead) return 'good';
    if (allGodsDead || allVillagersDead) return 'werewolf';
    return null;
}

function announceWinnerAndEndGame(winner) {
    const winnerText = winner === 'good' ? '🥳 好人阵营胜利！' : '🐺 狼人阵营胜利！';
    showModal(winnerText, [{text: '再来一局', class: 'btn-primary', handler: resetCurrentGame}]);
}

function showModal(text, buttons) {
    const modal = document.getElementById('modal');
    const modalText = document.getElementById('modal-text');
    const modalButtons = document.getElementById('modal-buttons');
    modalText.innerHTML = text;
    modalButtons.innerHTML = '';
    buttons.forEach(btnInfo => {
        const btn = document.createElement('button');
        btn.textContent = btnInfo.text;
        btn.className = `btn ${btnInfo.class}`;
        btn.onclick = () => {
            hideModal();
            btnInfo.handler();
        };
        modalButtons.appendChild(btn);
    });
    modal.classList.add('active');
}

function hideModal() {
    document.getElementById('modal').classList.remove('active');
}

function showAlert(text) {
    showModal(text, [{
        text: '好的', class: 'btn-primary', handler: () => {
        }
    }]);
}

function showConfirm(text, onConfirm, onCancel = () => {
}) {
    showModal(text, [{text: '取消', class: 'btn-secondary', handler: onCancel}, {
        text: '确定',
        class: 'btn-primary',
        handler: onConfirm
    }]);
}

function finalizeIdentities() {
    for (let i = 1; i <= gameState.config.total; i++) {
        if (!gameState.playerIdentities[i]) {
            gameState.playerIdentities[i] = 'civilian';
        }
    }
    addLog(`第一夜结束，所有未指认身份的玩家确认为平民。`);
}

function updateIdentitySummary() {
    const list = document.getElementById('identitySummaryList');
    list.innerHTML = '';
    const rolesInGame = ['wolfKing', 'werewolf', 'seer', 'witch', 'hunter', 'guard', 'civilian'];
    let html = '';
    rolesInGame.forEach(role => {
        let countInConfig;
        if (role === 'civilian') countInConfig = gameState.config.civilian; else if (role === 'werewolf') countInConfig = gameState.config.werewolf; else if (role === 'wolfKing') countInConfig = gameState.config.wolfKing ? 1 : 0; else countInConfig = gameState.config[role] ? 1 : 0;
        if (countInConfig === 0) return;
        const playersOfRole = Object.keys(gameState.playerIdentities).filter(p => gameState.playerIdentities[p] === role).sort((a, b) => parseInt(a) - parseInt(b));
        let playerSpans = playersOfRole.map(pNum => {
            const player = gameState.players.find(p => p.number == pNum);
            const statusClass = player.alive ? 'status-alive' : 'status-dead';
            return `<span class="${statusClass}">${pNum}号</span>`;
        }).join(' ');
        if (playersOfRole.length > 0 && playersOfRole.every(p => !isPlayerAlive(p))) {
            playerSpans += ` <span class="status-dead">(全部阵亡)</span>`;
        }
        list.innerHTML += `<div>${roleAvatars[role]} ${roleNames[role]}: ${playerSpans || '<span>未指定</span>'}</div>`;
    });
}

function populatePlayerSelector(containerId, onClickCallback, filterCallback = () => true, disableCallback = () => false) {
    const selector = document.getElementById(containerId);
    selector.innerHTML = '';
    gameState.players.filter(filterCallback).forEach(p => {
        const div = createPlayerNumber(p.number);
        const isDisabled = disableCallback(p);
        if (p.alive && !isDisabled) {
            div.onclick = () => onClickCallback(p.number);
        } else {
            div.classList.add(isDisabled ? 'disabled' : 'dead');
        }
        selector.appendChild(div);
    });
}

function createPlayerNumber(number) {
    const div = document.createElement('div');
    div.className = 'player-number';
    const role = gameState.playerIdentities[number];
    let avatarHTML = '';
    if (role && roleAvatars[role]) {
        avatarHTML = `<span class="player-avatar">${roleAvatars[role]}</span>`;
    }
    div.innerHTML = avatarHTML + number;
    div.dataset.number = number;
    const player = gameState.players.find(p => p.number === number);
    if (player && !player.alive) div.classList.add('dead');
    return div;
}

function killPlayer(playerNumber, reason) {
    const player = gameState.players.find(p => p.number == playerNumber);
    if (player && player.alive) {
        player.alive = false;
        gameState.deathQueue.push({number: playerNumber, reason: reason});
    }
}

function findRoleOwner(role) {
    for (const num in gameState.playerIdentities) {
        if (gameState.playerIdentities[num] === role) return parseInt(num);
    }
    return null;
}

function getPlayerByRole(role) {
    const owner = findRoleOwner(role);
    if (owner && isPlayerAlive(owner)) return owner;
    return null;
}

function isPlayerAlive(playerNum) {
    const player = gameState.players.find(p => p.number == playerNum);
    return player ? player.alive : false;
}

function hideAllActionCards() {
    ['actionCard', 'dayPhase', 'votingPhase', 'skillPhase'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none'
    });
}

function addLog(message) {
    const log = document.getElementById('gameLog');
    const item = document.createElement('div');
    item.innerHTML = `[${new Date().toLocaleTimeString()}] ${message}`;
    log.insertBefore(item, log.firstChild);
}

function clearLog() {
    document.getElementById('gameLog').innerHTML = '';
}

resetGameState();
document.getElementById('welcomePhase').classList.add('active');
updateTotalCount();