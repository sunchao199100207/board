// 自动化测试辅助：通过 URL 参数驱动页面自动创建/加入房间、选座、准备
// 支持参数：auto=create|join, room=9999, total=10, seat=1, ready=1, delayMs=200, nostorage=1
(function() {
    function getParam(name, def) {
        var v = new URLSearchParams(window.location.search).get(name);
        return v == null ? def : v;
    }

    var auto = getParam('auto', '');
    var room = getParam('room', '');
    var total = parseInt(getParam('total', '10'), 10) || 10;
    var seat = parseInt(getParam('seat', '0'), 10) || 0;
    var ready = getParam('ready', '0') === '1';
    var delayMs = parseInt(getParam('delayMs', '200'), 10) || 200;

    if (!auto || !room) return;

    function safeClick(elem) {
        if (elem && typeof elem.click === 'function') elem.click();
    }

    function doAutoJoin() {
        try {
            var inputRoom = document.getElementById('room-number');
            if (inputRoom) inputRoom.value = room;

            if (auto === 'create') {
                var slider = document.getElementById('total-players');
                if (slider) slider.value = String(total);
                setTimeout(function() {
                    safeClick(document.getElementById('btn-create-room'));
                }, 50);
            } else if (auto === 'join') {
                setTimeout(function() {
                    safeClick(document.getElementById('btn-join-room'));
                }, 50);
            }
        } catch (e) {
            console.error('[auto] 自动加入失败:', e);
        }
    }

    function trySeatAndReady() {
        try {
            if (seat > 0 && typeof window.selectSeat === 'function') {
                setTimeout(function() {
                    window.selectSeat(seat);
                }, 300);
            }
            if (ready) {
                setTimeout(function() {
                    var btnReady = document.getElementById('btn-ready');
                    if (btnReady && !btnReady.disabled) {
                        safeClick(btnReady);
                    }
                }, 900);
            }
        } catch (e) {
            console.error('[auto] 选座准备失败:', e);
        }
    }

    document.addEventListener('DOMContentLoaded', function() {
        setTimeout(function() {
            doAutoJoin();
        }, delayMs);
        setTimeout(function() {
            trySeatAndReady();
        }, delayMs + 600);
    });
})();
