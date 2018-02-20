define( ["react", "js/home/Home", "js/public/Validate", "js/anonymous/Validate"], function( React, Home, PublicValidate, AnonymousValidate ) {
	return class Main extends React.Component {
		constructor( props ) {
			super( props );

			this.renderSection = this.renderSection.bind( this );
			this.renderHome = this.renderHome.bind( this );
			this.renderPublic = this.renderPublic.bind( this );
			this.renderAnonymous = this.renderAnonymous.bind( this );
		}

		renderSection() {
			return React.createElement( 'div', { className: 'container main-container' }, this['render' + ( this.props.section )]() );
		}

		renderHome() {
			return React.createElement( Home, { alias: this.props.alias } );
		}

		renderPublic() {
			return React.createElement( PublicValidate, {} );
		}

		renderAnonymous() {
			return React.createElement( AnonymousValidate, {} );
		}

		render() {
			let className = 'main-inner' + ( ( this.props.vertical == true ) ? '-middle' : '' );
			return React.createElement( 'main', { className: className, role: 'main' }, this.renderSection() );
		}
	};
} );