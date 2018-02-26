define( ["react"], function( React ) {
	return class Footer extends React.Component {
		constructor( props ) {
			super( props );
		}

		render() {
			return React.createElement( 'footer', { className: 'mastfoot' },
				React.createElement( 'div', { className: 'inner' },
					React.createElement( 'p', null, this.props.body )
				)
			);
		}
	};
} );