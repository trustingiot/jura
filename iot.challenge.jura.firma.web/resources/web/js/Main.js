define(
	["react", "js/home/Home", "js/publicly/Validate", "js/anonymously/Validate", "js/admin/Admin"],
	function( React, Home, PubliclyValidate, AnonymouslyValidate, Admin ) {
		return class Main extends React.Component {
			constructor( props ) {
				super( props );

				this.renderSection = this.renderSection.bind( this );
				this.renderHome = this.renderHome.bind( this );
				this.renderPublicly = this.renderPublicly.bind( this );
				this.renderAnonymously = this.renderAnonymously.bind( this );
				this.renderAdmin = this.renderAdmin.bind( this );
			}

			renderSection() {
				return React.createElement( 'div', { className: 'container main-container' }, this['render' + ( this.props.section )]() );
			}

			renderHome() {
				return React.createElement( Home, { alias: this.props.alias } );
			}

			renderPublicly() {
				return React.createElement( PubliclyValidate, {} );
			}

			renderAnonymously() {
				return React.createElement( AnonymouslyValidate, {} );
			}

			renderAdmin() {
				return React.createElement( Admin, {} );
			}

			render() {
				let className = 'main-inner' + ( ( this.props.vertical == true ) ? '-middle' : '' );
				return React.createElement( 'main', { className: className, role: 'main' }, this.renderSection() );
			}
		};
	} );