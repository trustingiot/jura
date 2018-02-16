define( ["react", "js/firma/Firma"], function( React, Firma ) {
	return class Main extends React.Component {
		constructor( props ) {
			super( props );

			this.renderSection = this.renderSection.bind( this );
			this.renderFirma = this.renderFirma.bind( this );
		}

		renderSection() {
			return React.createElement( 'div', { className: 'container main-container' }, this['render' + ( this.props.section )]() );
		}

		renderFirma() {
			return React.createElement( Firma, {} );
		}

		render() {
			let className = 'main-inner' + ( ( this.props.vertical == true ) ? '-middle' : '' );
			return React.createElement( 'main', { className: className, role: 'main' },
				this.renderSection()
			);
		}
	};
} );