define( ["react"], function( React ) {
	return class Header extends React.Component {
		constructor( props ) {
			super( props );
		}

		render() {
			return React.createElement( 'header', { className: 'masthead clearfix' },
				React.createElement( 'div', { className: 'inner' },
					React.createElement( 'div', { className: 'logo' } ),
					React.createElement( 'h3', { className: 'masthead-brand' }, this.props.title )
				)
			);
		}
	};
} );