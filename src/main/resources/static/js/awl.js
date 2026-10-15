// --- 配置 ---
const API_URL = 'api/get-identity'; // 相对路径

// identitiesMap 仍然需要，用于前端的输入验证
const identitiesMap = {
    5: ['梅林', '派西维尔', '亚瑟的忠臣', '莫甘娜', '刺客'],
    6: ['梅林', '派西维尔', '亚瑟的忠臣', '莫甘娜', '刺客', '亚瑟的忠臣'],
    7: ['梅林', '派西维尔', '亚瑟的忠臣', '莫甘娜', '刺客', '亚瑟的忠臣', '奥伯伦'],
    8: ['梅林', '派西维尔', '亚瑟的忠臣', '莫甘娜', '刺客', '亚瑟的忠臣', '亚瑟的忠臣', '莫德雷德的爪牙'],
    9: ['梅林', '派西维尔', '亚瑟的忠臣', '莫甘娜', '刺客', '亚瑟的忠臣', '亚瑟的忠臣', '亚瑟的忠臣', '莫德雷德'],
    10: ['梅林', '派西维尔', '亚瑟的忠臣', '莫甘娜', '刺客', '亚瑟的忠臣', '亚瑟的忠臣', '亚瑟的忠臣', '莫德雷德', '奥伯伦']
};

// roleInfo 对象已删除，不再需要

// 主函数：获取身份 (async/await)
async function getIdentity(event) {
    event.preventDefault();
    const submitButton = document.getElementById('submit-button');

    const form = event.target;
    const playersNumber = parseInt(form.PlayersNumber.value);
    const gameNumber = parseInt(form.GameNumber.value);
    const seatNumber = parseInt(form.SeatNumber.value);

    // --- 输入验证 ---
    if (!playersNumber || !gameNumber || !seatNumber) {
        showError('所有字段均为必填项。');
        return;
    }
    // identitiesMap 仍然在这里用于验证
    if (!identitiesMap[playersNumber]) {
        showError('总人数必须在 5 到 10 之间。');
        return;
    }
    if (seatNumber < 1 || seatNumber > playersNumber) {
        showError(`座位号必须在 1 到 ${playersNumber} 之间。`);
        return;
    }
    hideError();

    submitButton.disabled = true;
    submitButton.textContent = '获取中...';

    // --- 核心逻辑: API 调用 ---
    try {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                playersNumber: playersNumber,
                gameNumber: gameNumber,
                seatNumber: seatNumber,
            }),
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || '发生未知错误');
        }

        // --- 成功：更新 UI ---
        document.getElementById('identity-card').className = `card ${data.camp}`;
        document.getElementById('identity-image').src = data.imageUrl;
        document.getElementById('identity-name').textContent = data.identity;

        document.getElementById('seen-title').textContent = data.seenTitle;
        document.getElementById('seen-players').textContent = data.seenPlayers.length > 0 ? data.seenPlayers.join(', ') : '—';
        document.getElementById('seen-subtitle').textContent = data.seenSubtitle;

        showResult();

    } catch (error) {
        if (error.message.includes('Failed to fetch')) {
            showError('无法连接到服务器。请确保后端已运行。');
        } else {
            showError(error.message);
        }
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = '获取身份';
    }
}

// --- updateRoleDisplay 函数已删除 ---

// --- UI 控制函数 ---
function showResult() {
    document.getElementById('form-container').style.display = 'none';
    document.getElementById('result-container').style.display = 'block';
}

function showForm() {
    document.getElementById('form-container').style.display = 'block';
    document.getElementById('result-container').style.display = 'none';

    hideError();

    const gameNumberInput = document.getElementById('game-number');
    if (gameNumberInput) {
        gameNumberInput.value = '';
    }

    // 对 updateRoleDisplay 的调用已删除
}

function showError(message) {
    const errorDiv = document.getElementById('error-message');
    errorDiv.textContent = message;
    errorDiv.style.display = 'block';
}

function hideError() {
    const errorDiv = document.getElementById('error-message');
    if (errorDiv) {
        errorDiv.style.display = 'none';
    }
}

// --- 绑定事件 ---
document.addEventListener('DOMContentLoaded', (event) => {
    const identityForm = document.getElementById('identity-form');
    if (identityForm) {
        identityForm.addEventListener('submit', getIdentity);
    }

    // “总人数”输入框的 'input' 事件监听器已删除

    const backButton = document.getElementById('back-button');
    if (backButton) {
        backButton.addEventListener('click', showForm);
    }

    // --- 页面初始化 ---
    // 对 updateRoleDisplay 的调用已删除

    const formContainer = document.getElementById('form-container');
    const resultContainer = document.getElementById('result-container');

    if(formContainer) formContainer.style.display = 'block';
    if(resultContainer) resultContainer.style.display = 'none';
});