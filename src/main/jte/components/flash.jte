@param String message
@param String type

<div
    class="flash-message flash-message-${type}"
    role="${"error".equals(type) ? "alert" : "status"}"
    data-flash-message
    data-auto-dismiss-ms="10000"
>
    <span class="flash-message-text">${message}</span>

    <button
        class="flash-close"
        type="button"
        aria-label="Закрыть сообщение"
        data-flash-close
    >
        ×
    </button>
</div>
