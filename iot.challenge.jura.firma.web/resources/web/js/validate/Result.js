define( ["react"], function( React ) {
	return class Result extends React.Component {
		constructor( props ) {
			super( props );

			this.renderTitle = this.renderTitle.bind( this );
			this.renderBody = this.renderBody.bind( this );
		}

		renderTitle() {
			var transaction = this.props.validationResult.transaction;
			var title = ( transaction !== undefined ) ?
				['Transaction ',
					React.createElement( 'a', { key: 'transaction', href: 'https://thetangle.org/transaction/' + transaction, target: '_blank' }, transaction )]
				: '';
			var loadingClassName = 'loading loading-' + ( ( this.props.processing == true ) ? 'active' : 'inactive' );
			return React.createElement( 'div', { key: 'title', className: 'validateResultTitle' },
				React.createElement( 'h6', { key: 'transaction' }, title ),
				React.createElement( 'div', { key: 'indicator', className: loadingClassName } )
			);
		}

		renderBody() {
			if ( this.props.processing )
				return [];

			var transaction = this.props.validationResult.transaction;
			var reject = this.props.validationResult.reject;
			var sign = this.props.validationResult.sign;

			if ( reject === undefined && sign === undefined )
				return [];

			return React.createElement( 'div', { key: 'result-body', className: 'result-body' },
				( reject !== undefined ) ?
					React.createElement( 'blockquote', { key: 'result', className: 'blockquote' },
						React.createElement( 'p', { key: 'reject', className: 'sign-header' }, reject ) )
					: this.renderSign( sign ) );
		}

		renderSign( sign ) {
			var timestamp = new Date( Number( sign.body.timestamp ) );
			var device = sign.body.location.device;
			var installation = sign.body.location.installation;
			var x = sign.body.location.point.X;
			var y = sign.body.location.point.Y;
			var id = sign.sign.key;

			return React.createElement( 'blockquote', { key: 'result', className: 'blockquote' },
				React.createElement( 'p', { key: 'sign-header', className: 'sign-header' },
					'The owner of the PGP key ',
					React.createElement( 'a', { key: 'transaction', href: 'https://pgp.mit.edu/pks/lookup?op=vindex&search=0x' + id, target: '_blank' }, '0x' + id ),
					' certifies that:' ),
				React.createElement( 'p', { key: 'message' },
					'Device ',
					React.createElement( 'b', { key: 'device' }, device ),
					' was in location ',
					React.createElement( 'b', { key: 'location' }, '[x:' + x + ', y:' + y + ']' ),
					' of installation ',
					React.createElement( 'b', { key: 'installation' }, installation ),
					' at time ',
					React.createElement( 'b', { key: 'timestamp' }, timestamp.toLocaleDateString() + ' ' + timestamp.toLocaleTimeString() ) )
			);
		}

		render() {
			return React.createElement( 'div', { className: 'container validation-result' },
				this.renderTitle(),
				this.renderBody()
			);
		}
	};
} );