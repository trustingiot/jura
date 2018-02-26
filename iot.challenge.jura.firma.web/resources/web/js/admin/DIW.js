define( ["react"], function( React ) {
	return class DIW extends React.Component {
		constructor( props ) {
			super( props );

			this.submitForm = this.submitForm.bind( this );
		}

		renderInput( id, type, placeholder ) {
			return React.createElement( 'div', { key: id, className: 'form-group table-row' },
				React.createElement( 'input', { key: id, type: type, className: 'form-control', id: id, placeholder: placeholder, ref: ( v ) => this[id] = v } )
			);
		}

		submitForm() {
			let d = this.device.value.trim();
			let p = this.password.value.trim();
			if ( d != '' && p != '' ) {
				let s = Math.random().toString().substring( 2 );
				let sha = new jsSHA( 'SHA-256', 'TEXT' );
				sha.update( d + p + s );
				this.props.post( 'diw', {
					device: d,
					seed: s,
					value: sha.getHash( 'B64' )
				}, this.props.onDIW );
			}
		}

		handleSubmit( event ) {
			event.preventDefault();
		}

		createDIWField( diw, device ) {
			return React.createElement( 'div', { className: 'field', key: 'diw' },
				React.createElement( 'span', { className: 'field-label' }, ( device == null ) ? 'DIW' : 'DIW for [' + device + ']' ),
				React.createElement( 'span', { className: ( diw == null ) ? 'field-value field-value-none' : 'field-value' }, ( diw == null ) ? 'None' : diw )
			);
		}

		render() {
			return React.createElement( 'div', { className: 'container submit-form' },
				React.createElement( 'form', { onSubmit: this.handleSubmit },
					this.renderInput( 'device', 'text', 'Device MAC' ),
					this.renderInput( 'password', 'password', 'Admin password' ),
					React.createElement( 'button', { type: 'submit', className: 'btn btn-dark btn-block', onClick: this.submitForm }, 'Obtain Device Identification Word' )
				),
				this.createDIWField( this.props.diw, ( this.device !== undefined ) ? this.device.value : null )
			);

		}
	};
} );