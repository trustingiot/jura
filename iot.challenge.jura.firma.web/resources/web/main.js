requirejs.config( {
	paths: {
		'react': 'https://unpkg.com/react@16/umd/react.development',
		'react-dom': 'https://unpkg.com/react-dom@16/umd/react-dom.development',
		'js': 'firma/js'
	}
} );

requirejs( ['react', 'react-dom', 'js/Application'], function( React, ReactDOM, Application ) {
	ReactDOM.render(
		React.createElement( Application, null ),
		document.getElementById( 'root' )
	);
} );