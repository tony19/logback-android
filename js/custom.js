// assumes Jquery is loaded at this point

$(function() {

	$(".expander").expander();

	$("a#shot_hello").fancybox({
		'overlayShow'	: false,
		'titlePosition'	: 'inside',
		'transitionIn'	: 'elastic',
		'transitionOut'	: 'elastic'
	});

});

$.trackPage('UA-27964159-1')