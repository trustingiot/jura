define( ["react"], function( React ) {
	return class Nav extends React.Component {
		constructor( props ) {
			super( props );

			this.setSection = this.setSection.bind( this );
		}

		setSection( e ) {
			this.props.setSection( e.target.text );
		}

		render() {
			const sectionLinks = this.props.sections.map(( section ) =>
				React.createElement( 'a', { key: section, className: 'nav-link' + ( this.props.section == section ? ' active' : '' ), href: '#', onClick: ( e ) => this.setSection( e ) }, section )
			);
			return React.createElement( 'nav', { className: 'nav nav-masthead' }, sectionLinks );
		}
	};
} );