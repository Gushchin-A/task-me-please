@import dev.gushchin.taskmanager.view.EmailContentView
@import dev.gushchin.taskmanager.view.EmailParameterView

@param String serviceUrl
@param EmailContentView content

<!doctype html>
<html lang="ru" translate="no">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${content.getHeading()}</title>
<style>
  body { margin: 0; padding: 0; background-color: #dde3e8; }
  .tmp-footer-link:hover { text-decoration: underline !important; }
  @media only screen and (max-width: 620px) {
    .tmp-page-pad { padding: 20px 12px !important; }
    .tmp-card-pad { padding: 24px 20px !important; }
    .tmp-card { max-width: 100% !important; }
    .tmp-title { font-size: 18px !important; }
    .tmp-box-top { padding: 20px 20px 16px !important; }
    .tmp-box-bottom { padding: 16px 20px 20px !important; }
    .tmp-box-single { padding: 20px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background-color:#dde3e8; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif,'Apple Color Emoji','Segoe UI Emoji';">

  <div style="max-width:640px; margin:0 auto; padding:32px 16px;" class="tmp-page-pad">

    <table role="presentation" border="0" width="100%" cellpadding="0" cellspacing="0" style="border-collapse:separate; border-spacing:0; max-width:560px; margin:0 auto; background-color:#ffffff; border:1px solid #d1d9e0; border-radius:6px;" class="tmp-card">
      <tr>
        <td class="tmp-card-pad" style="padding:40px 32px;">

          <table role="presentation" border="0" width="100%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
            <tr>
              <td align="center" style="padding-bottom:24px;">
                <span translate="no" class="notranslate" style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif; font-size:17px; font-weight:600; letter-spacing:-0.02em; color:#1f2328;">TASKMEPLEASE</span>
              </td>
            </tr>
            <tr>
              <td align="center" style="padding-bottom:24px;">
                <h1 class="tmp-title" style="margin:0; font-size:20px; line-height:1.3; font-weight:600; color:#1f2328;">${content.getHeading()}</h1>
              </td>
            </tr>
          </table>

          <div style="border:1px solid #d1d9e0; border-radius:6px; overflow:hidden;">
          <table role="presentation" border="0" width="100%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
            <tr>
              <td class="${content.hasNotes() ? "tmp-box-top" : "tmp-box-single"}" style="padding:${content.hasNotes() ? "24px 24px 20px" : "24px"};">
                @for(int i = 0; i < content.getBodyParagraphs().size(); i++)
                  <p style="margin:0 0 ${i == content.getBodyParagraphs().size() - 1 ? content.bodyMarginBottom() : "12px"}; font-size:14px; line-height:1.5; color:#1f2328;">${content.getBodyParagraphs().get(i)}</p>
                @endfor
                @if(content.hasParameters())
                <p style="margin:0 0 ${content.parametersMarginBottom()}; font-size:14px; line-height:1.5; color:#1f2328;">
                  @for(int i = 0; i < content.getParameters().size(); i++)
                    !{EmailParameterView parameter = content.getParameters().get(i);}
                    <b style="font-weight:600;">${parameter.label()}:</b> ${parameter.value()}@if(i < content.getParameters().size() - 1)<br>@endif
                  @endfor
                </p>
                @endif
                @if(content.hasAction())
                <table role="presentation" border="0" cellpadding="0" cellspacing="0" style="border-collapse:separate; border-spacing:0; margin:0 auto;">
                  <tr>
                    <td align="center" bgcolor="#1f883d" style="border-radius:6px; border:1px solid #1f232826;">
                      <a href="${content.getAction().url()}" target="_blank" style="display:inline-block; padding:12px 28px; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif; font-size:14px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:6px;">${content.getAction().label()}</a>
                    </td>
                  </tr>
                </table>
                @endif
              </td>
            </tr>
            @if(content.hasNotes())
            <tr>
              <td style="padding:0; line-height:0; font-size:0; border-top:1px solid #d1d9e0b3;">&nbsp;</td>
            </tr>
            <tr>
              <td class="tmp-box-bottom" style="padding:20px 24px 24px;">
                @for(int i = 0; i < content.getNotes().size(); i++)
                  <p style="margin:0 0 ${i == content.getNotes().size() - 1 ? "0" : "12px"}; font-size:12px; line-height:1.5; color:#59636e;">${content.getNotes().get(i)}</p>
                @endfor
              </td>
            </tr>
            @endif
          </table>
          </div>

          <table role="presentation" border="0" width="100%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
            <tr>
              <td align="center" style="padding-top:32px;">
                <a href="${serviceUrl}" target="_blank" translate="no" class="tmp-footer-link notranslate" style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif; font-size:11px; color:#59636e; text-decoration:none;">TaskMePlease</a>
              </td>
            </tr>
          </table>

        </td>
      </tr>
    </table>

  </div>
</body>
</html>
