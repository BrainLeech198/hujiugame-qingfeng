<template>
    <footer class="footnote">
        <div class="footer-inner">
            <div class="footer-col footer-brand">
                <span class="footer-logo">
                    <span>{{ $t('footer_text') }}</span>
                </span>
                <span class="footer-copy">&copy; HujiuGame</span>
            </div>
            <div class="footer-col footer-legal">
                <router-link to="/copyright">{{ $t('footer_copyright') }}</router-link>
            </div>
        </div>
    </footer>
</template>
