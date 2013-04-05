// JIRA integration
// Requires jQuery!
jQuery.ajax({
    url: "http://matisse.cs.umass.edu:9000/s/en_US-vo5e90-418945332/813/3/1.2.9/_/download/batch/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector-embededjs/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector-embededjs.js?collectorId=a72f13e9",
    type: "get",
    cache: true,
    dataType: "script"
});

// enable Bootstrap tooltips
function fixTooltip(selector) {
    $(selector + " a").tooltip({
        'selector': '',
        'placement': 'top'
    });
    $(selector + " .withtooltip").tooltip({
        'selector': '',
        'placement': 'top'
    });
    //console.log(selector + " a");
};

function expandInit() {
    var reducedHeight = $(this).height();
    $(this).css('height', 'auto');
    var fullHeight = $(this).height();
    $(this).height(reducedHeight);

    $(this).data('reducedHeight', reducedHeight);
    $(this).data('fullHeight', fullHeight);
};

function expandOnClick() {
    readmore = $(this).attr('read-more')
    readless = $(this).attr('read-less')
    
    $(this).animate({height: $(this).height() == $(this).data('reducedHeight') ? $(this).data('fullHeight') : $(this).data('reducedHeight')}, 200);
    $(this).toggleClass('cursor-down cursor-up', 200);
    $(this).prop('title', function (idx, oldAttr) {
        console.log("oldAttr = " + oldAttr);
        console.log("readmore = " + readmore);
        console.log(oldAttr == readmore)
        if (oldAttr == readmore) {
            t = $(this).attr('read-less')
        } else {
            t = $(this).attr('read-more')
        };

        console.log("newAttr = " + t);
        return t
    });
    $(this).children('.read-more-toggle').toggleClass('read-more read-more-off', 200);
    $(element).tooltip('fixTitle').tooltip('show');
};

$(document).ready(function () {
    fixTooltip("");

    /*
     $('.accordion').collapse();

     $('.accordion').on('show hide', function(e){
     $(e.target).siblings('.accordion-heading').find('.accordion-toggle i').toggleClass('icon-arrow-down icon-arrow-up', 200);
     });
     */




    $('.expand').each(function () {
        var reducedHeight = $(this).height();
        $(this).css('height', 'auto');
        var fullHeight = $(this).height();
        $(this).height(reducedHeight);

        $(this).data('reducedHeight', reducedHeight);
        $(this).data('fullHeight', fullHeight);
    }).click(function () {
            readmore = $(this).attr('read-more')
            readless = $(this).attr('read-less')
            
            $(this).animate({height: $(this).height() == $(this).data('reducedHeight') ? $(this).data('fullHeight') : $(this).data('reducedHeight')}, 200);
            $(this).toggleClass('cursor-down cursor-up', 200);
            $(this).prop('title', function (idx, oldAttr) {
                console.log("oldAttr = " + oldAttr);
                console.log("readmore = " + readmore);
                console.log(oldAttr == readmore)
                if (oldAttr == readmore) {
                    t = $(this).attr('read-less')
                } else {
                    t = $(this).attr('read-more')
                }
                ;

                console.log("newAttr = " + t);
                return t
            });
            $(this).children('.read-more-toggle').toggleClass('read-more read-more-off', 200);
            $(element).tooltip('fixTitle').tooltip('show');
        });
});
