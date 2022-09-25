import './Detail.css';
import React from 'react';
import Tabs from 'react-bootstrap/Tabs'
import Tab from 'react-bootstrap/Tab'

class Detail extends React.Component {

  componentDidMount() {
  }  

  render() {
    if (this.props.variant == null) {
      return <div className="helpBanner">
              <h1>VCF-Visualize</h1>
              <b>
                Select a variant!
              </b><br/>
              <i>After selecting you can use the arrow keys to quickly search through variants</i>
            </div>
    }

    //return <b>{this.props.variant.pos}</b>

    return <div className="detailContainer"><Tabs defaultActiveKey="home" id="uncontrolled-tab-example" className="mb-3">
      <Tab eventKey="home" title="Data">
        <div className="tabContent">
          <table className="detail">
            <tr>
              <td className="key">
                Chromosome
              </td>
              <td className="value">
                {this.props.variant.chrom}
              </td>
            </tr>
            <tr>
              <td className="key">
                Position
              </td>
              <td className="value">
                {this.props.variant.pos}
              </td>
            </tr>
            <tr>
              <td className="key">
                Referece
              </td>
              <td className="value">
                {this.props.variant.ref}
              </td>
            </tr>
            <tr>
              <td className="key">
                Alternative
              </td>
              <td className="value">
                {this.props.variant.alt}
              </td>
            </tr>
            <tr>
              <td className="key">
                Quality
              </td>
              <td className="value">
                {this.props.variant.qual}
              </td>
            </tr>
            <tr>
              <td className="key">
                Filter
              </td>
              <td className="value">
                {this.props.variant.filter}
              </td>
            </tr>
            <tr>
              <td className="key">
                Format
              </td>
              <td className="value">
                {this.props.variant.format}
              </td>
            </tr>
          </table>
          
        </div>
      </Tab>
      <Tab eventKey="profile" title="IGV">
        <div className="tabContent">
          IGV
        </div>
      </Tab>
      <Tab eventKey="contact" title="Something">
        <div className="tabContent">
          TODO: Lolliplot
        </div>
      </Tab>
    </Tabs></div>
  }
}

export default Detail;
