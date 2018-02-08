define( ["react", "js/home/Home", "js/scanner/Scanner", "js/graba/Graba"], function( React, Home, Scanner, Graba ) {
	return class Main extends React.Component {
		constructor( props ) {
			super( props );

			this.state = { info: null };

			this.setInfo = this.setInfo.bind( this );
			this.renderInfo = this.renderInfo.bind( this );
			this.hideInfo = this.hideInfo.bind( this );
			this.renderSection = this.renderSection.bind( this );
			this.renderHome = this.renderHome.bind( this );
			this.renderSneak = this.renderSneak.bind( this );
			this.renderGraba = this.renderGraba.bind( this );
		}

		setInfo( info ) {
			this.setState( { info: info } );
		}

		renderInfo() {
			if ( this.state.info == null ) return null;

			return React.createElement( 'div', { className: 'container', id: 'info-container' },
				React.createElement( 'div', { className: 'alert alert-info alert-dismissible fade show' },
					React.createElement( 'button', { type: 'button', className: 'close' },
						React.createElement( 'span', { onClick: this.hideInfo }, 'X' )
					),
					this.state.info
				)
			);
		}

		hideInfo() {
			this.setInfo( null );
		}

		renderSection() {
			return React.createElement( 'div', { className: 'container main-container' }, this['render' + ( this.props.section )]() );
		}

		renderHome() {
			return React.createElement( Home, { setInfo: this.setInfo, alias: this.props.alias } );
		}

		renderSneak() {
			return React.createElement( Scanner, { setInfo: this.setInfo } );
		}

		renderGraba() {
			return React.createElement( Graba, { setInfo: this.setInfo } );
		}

		render() {
			let className = 'main-inner' + ( ( this.props.vertical == true ) ? '-middle' : '' );
			return React.createElement( 'main', { className: className, role: 'main' },
				this.renderInfo(),
				this.renderSection()
			);
		}
	};
} );