$(function () {
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

    if (!csrfToken || !csrfHeader) {
        return;
    }

    $.ajaxSetup({
        beforeSend: function (xhr, settings) {
            if (settings.type && settings.type.toUpperCase() !== 'GET') {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        }
    });
});