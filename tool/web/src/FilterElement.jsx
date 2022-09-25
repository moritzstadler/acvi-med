import './FilterElement.css';
import React from 'react';

export class FilterElement extends React.Component {

  componentDidMount() {
  }  

  render() {
    return <div>
        {this.props.data.name}
        {this.props.data.operator}
        {this.props.data.value}
      </div>
  }
}

export default FilterElement;
