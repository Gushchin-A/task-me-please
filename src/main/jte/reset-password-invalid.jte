@template.layout.auth(
    title = "Ссылка недействительна или устарела",
    loginUrl = "/login",
    registrationUrl = "/registration",
    activeTab = "reset-password-invalid",
    invite = null,
    inviteContext = null,
    successMessage = null,
    errorMessage = null,
    content = @`
        <p class="auth-description">Запросите новую ссылку восстановления пароля.</p>
        <a class="auth-submit" href="/forgot-password">Запросить новую ссылку</a>
    `
)
