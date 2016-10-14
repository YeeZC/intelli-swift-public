import mixin from 'react-mixin'
import ReactDOM from 'react-dom'
import Immutable from 'immutable'
import {immutableShallowEqual, ReactComponentWithImmutableRenderMixin, requestAnimationFrame} from 'core'
import React, {
    Component,
    StyleSheet,
    Text,
    Dimensions,
    ListView,
    View,
    Fetch,
    Portal
} from 'lib'

import {Size, TemplateFactory, WidgetFactory} from 'data'
import {Layout} from 'layout'
import {Table, Dialog, IconLink} from 'base'
import {TableWidget} from 'widgets';

import DetailTableComponent from './DetailTableComponent';

import SettingsComponent from '../Settings/SettingsComponent'


class DetailTablePaneComponent extends Component {
    static contextTypes = {
        $template: React.PropTypes.object,
        actions: React.PropTypes.object
    };

    constructor(props, context) {
        super(props, context);
    }

    componentWillMount() {

    }

    componentDidMount() {

    }

    componentWillUpdate() {

    }

    _renderHeader() {
        const {$widget, wId} = this.props;
        const widget = WidgetFactory.createWidget($widget, wId, TemplateFactory.createTemplate(this.context.$template));
        return <Layout main='justify' cross='center' height={Size.HEADER_HEIGHT} style={styles.header}>
            <Text>{widget.getName()}</Text>
            <IconLink className='setting-font' onPress={()=> {
                Portal.showModal('DetailTableComponent', <SettingsComponent
                    $widget={this.props.$widget}
                    wId={this.props.wId}
                    height={0}
                    onComplete={(opt)=> {
                        Portal.closeModal('DetailTableComponent');
                        this.context.actions.updateWidget(opt.$widget, opt.wId);
                    }}
                    onReturn={()=> {
                        Portal.closeModal('DetailTableComponent');
                    }}
                />);
            }}/>
        </Layout>
    }

    render() {
        const {width, height, $widget, wId} = this.props;
        return <Layout dir='top' box='first'>
            {this._renderHeader()}
            <DetailTableComponent
                width={width}
                height={height - Size.HEADER_HEIGHT}
                $widget={$widget}
                wId={wId}
            >
            </DetailTableComponent>
        </Layout>
    }
}
mixin.onClass(DetailTablePaneComponent, ReactComponentWithImmutableRenderMixin);

const styles = StyleSheet.create({
    wrapper: {
        position: 'relative'
    },
    header: {
        paddingLeft: 4,
        paddingRight: 4,
        height: Size.HEADER_HEIGHT
    }
});
export default DetailTablePaneComponent
