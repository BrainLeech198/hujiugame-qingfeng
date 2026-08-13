/* ================================
   氢风官网 · LICENSE 许可页标签切换
   ================================ */
(function () {
    const btns = document.querySelectorAll('.tab-btn');
    btns.forEach(function (btn) {
        btn.addEventListener('click', function () {
            btns.forEach(function (b) { b.classList.remove('active'); });
            btn.classList.add('active');
            const target = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-panel').forEach(function (p) {
                p.classList.toggle('active', p.id === 'tab-' + target);
            });
        });
    });
})();
