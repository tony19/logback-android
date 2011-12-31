// assumes Jquery is loaded at this point

$(function() {

	$(".expander").expander();
	$(".zoom").fancybox({
		'titlePosition'	: 'inside',
		'transitionIn'	: 'elastic',
		'transitionOut'	: 'elastic',
		'overlayColor'	: '#000',
		'overlayOpacity': 0.8
	});

	$("a#changelog").fancybox();
});

$.trackPage('UA-27964159-1')