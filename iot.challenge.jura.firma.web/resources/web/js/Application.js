define( ["react", "js/header/Header", "js/Main", "js/Footer"], function( React, Header, Main, Footer ) {
	return class Application extends React.Component {
		constructor( props ) {
			super( props );

			this.sections = ['Home', 'Public', 'Anonymous'];
			this.verticalSections = ['Home'];

			this.state = { section: 'Home' };

			this.setSection = this.setSection.bind( this );
		}

		setSection( section ) {
			if ( this.state.section != section )
				this.setState( { section: section } );
		}

		render() {
			let vertical = this.verticalSections.includes( this.state.section );
			let className = 'site-wrapper-inner' + ( vertical ? '-middle' : '' );
			return React.createElement( 'div', { className: className },
				React.createElement( Header, { title: 'Firma', setSection: this.setSection, section: this.state.section, sections: this.sections } ),
				React.createElement( Main, { alias: 'Firma web', section: this.state.section, vertical: vertical } ),
				React.createElement( Footer, { body: 'Open IoT Challenge 4.0' } )
			);
		}
	};
} );