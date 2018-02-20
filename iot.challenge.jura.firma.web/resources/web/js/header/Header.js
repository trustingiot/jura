define( ["react", "js/header/Nav"], function( React, Nav ) {
	return class Header extends React.Component {
		constructor( props ) {
			super( props );
		}

		render() {
			return React.createElement( 'header', { className: 'masthead clearfix' },
				React.createElement( 'div', { className: 'inner' },
					React.createElement( 'div', { className: 'logo' } ),
					React.createElement( 'h3', { className: 'masthead-brand' }, this.props.title ),
					React.createElement( Nav, { setSection: this.props.setSection, section: this.props.section, sections: this.props.sections } )
				)
			);
		}
	};
} );