// window.onscroll = function() {
//     var bannerheight=$("#banner").height();
//     var scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
//     if(scrollTop>bannerheight){
//         $("#hidenav").css("zIndex", -1);
//     }else{
//         $("#hidenav").css("zIndex", 11);
//     }
// }
$(document).ready(function () {
	return;
    $('img').lazyload({ threshold :500});
   
    $('#sum').text($('.bbs').length);

    $(function () { 
	    var red = "01,02,07,08,12,13,18,19,23,24,29,30,34,35,40,45,46";
	    var blue = "03,04,09,10,14,15,20,25,26,31,36,37,41,42,47,48";
	    var green = "05,06,11,16,17,21,22,27,28,32,33,38,39,43,44,49";

    	var list = $('.balls div');
		[...list ].forEach(el => {
		  const $el = $(el);
		  const val = $el.text();
		  if (red.indexOf(val)>-1) {
		    $el.addClass('bo-red')
		  } else if (blue.indexOf(val)>-1) {
		    $el.addClass('bo-blue')
		  } else if (green.indexOf(val)>-1) {
		    $el.addClass('bo-green')
		  }
		});
	})
});

// ·µ»Ø¶¥²¿
var jscroll_to_top = $('#scroll_to_top');
$(window).scroll(function() {
	if ($(window).scrollTop() >= 500) {
	    jscroll_to_top.fadeIn(300);
	} else {
	    jscroll_to_top.fadeOut(300);
	}
});

jscroll_to_top.on('click', function() {
	$('html,body').animate({scrollTop: '0px' }, 100);
});