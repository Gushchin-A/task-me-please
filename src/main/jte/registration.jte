@import org.springframework.security.web.csrf.CsrfToken

@param CsrfToken _csrf

<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Registration</title>
</head>
<body>
    <h1>Registration</h1>

    <form method="post" action="/registration">
        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

        <div>
            <label>Email</label>
            <input type="email" name="email" required>
        </div>

        <div>
            <label>Password</label>
            <input type="password" name="password" required>
        </div>

        <button type="submit">Create account</button>
    </form>
</body>
</html>