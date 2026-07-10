/* logback-android interactive config editor
 *
 * Maintains a JSON model of the configuration (settings, properties,
 * appenders, loggers) and regenerates a highlighted logback.xml preview
 * on every change. State persists to localStorage.
 */
(function () {
  'use strict';

  var STORAGE_KEY = 'lba-editor-state-v1';
  var LEVELS = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'];
  var FILTER_LEVELS = ['', 'TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'];
  var SYSLOG_FACILITIES = ['USER', 'AUTH', 'AUTHPRIV', 'DAEMON', 'SYSLOG', 'LOCAL0', 'LOCAL1', 'LOCAL2', 'LOCAL3', 'LOCAL4', 'LOCAL5', 'LOCAL6', 'LOCAL7'];

  // ---------------------------------------------------------------------
  // Appender type definitions: option fields + XML generation
  // ---------------------------------------------------------------------

  var APPENDER_TYPES = {
    logcat: {
      label: 'Logcat',
      className: 'ch.qos.logback.classic.android.LogcatAppender',
      defaults: { tagPattern: '%logger{12}', pattern: '[%-20thread] %msg', checkLoggable: false },
      fields: [
        { key: 'tagPattern', label: 'Tag pattern', type: 'text' },
        { key: 'pattern', label: 'Message pattern', type: 'text' },
        { key: 'checkLoggable', label: 'Honor Log.isLoggable() tag levels', type: 'bool' }
      ],
      body: function (o) {
        var el = [];
        if (o.checkLoggable) el.push(tag('checkLoggable', 'true'));
        el.push(wrap('tagEncoder', [tag('pattern', o.tagPattern)]));
        el.push(wrap('encoder', [tag('pattern', o.pattern)]));
        return el;
      }
    },

    file: {
      label: 'File',
      className: 'ch.qos.logback.core.FileAppender',
      defaults: { file: '${LOG_DIR}/app.log', append: true, lazy: false, pattern: '%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n' },
      fields: [
        { key: 'file', label: 'File path', type: 'text' },
        { key: 'pattern', label: 'Message pattern', type: 'text' },
        { key: 'append', label: 'Append to existing file', type: 'bool' },
        { key: 'lazy', label: 'Lazy file creation (on first log)', type: 'bool' }
      ],
      body: function (o) {
        var el = [tag('file', o.file)];
        if (!o.append) el.push(tag('append', 'false'));
        if (o.lazy) el.push(tag('lazy', 'true'));
        el.push(wrap('encoder', [tag('pattern', o.pattern)]));
        return el;
      }
    },

    rolling: {
      label: 'Rolling file',
      className: 'ch.qos.logback.core.rolling.RollingFileAppender',
      defaults: {
        file: '${LOG_DIR}/app.log',
        fileNamePattern: '${LOG_DIR}/app.%d{yyyy-MM-dd}.%i.log',
        maxHistory: '7',
        maxFileSize: '5MB',
        pattern: '%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n'
      },
      fields: [
        { key: 'file', label: 'Active file path', type: 'text' },
        { key: 'fileNamePattern', label: 'Rolled file name pattern', type: 'text' },
        { key: 'maxHistory', label: 'Max history (rolled files kept)', type: 'text', size: 'small' },
        { key: 'maxFileSize', label: 'Max file size (blank = time-based only)', type: 'text', size: 'small' },
        { key: 'pattern', label: 'Message pattern', type: 'text' }
      ],
      body: function (o) {
        var policy = [tag('fileNamePattern', o.fileNamePattern)];
        if (o.maxHistory) policy.push(tag('maxHistory', o.maxHistory));
        if (o.maxFileSize) {
          policy.push(wrap('timeBasedFileNamingAndTriggeringPolicy',
            [tag('maxFileSize', o.maxFileSize)],
            { class: 'ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP' }));
        }
        return [
          tag('file', o.file),
          wrap('rollingPolicy', policy, { class: 'ch.qos.logback.core.rolling.TimeBasedRollingPolicy' }),
          wrap('encoder', [tag('pattern', o.pattern)])
        ];
      }
    },

    sqlite: {
      label: 'SQLite',
      className: 'ch.qos.logback.classic.android.SQLiteAppender',
      defaults: { filename: 'logs.db', maxHistory: '7 days' },
      fields: [
        { key: 'filename', label: 'Database file name (blank = default)', type: 'text' },
        { key: 'maxHistory', label: 'Max history (e.g., "7 days")', type: 'text', size: 'small' }
      ],
      body: function (o) {
        var el = [];
        if (o.filename) el.push(tag('filename', o.filename));
        if (o.maxHistory) el.push(tag('maxHistory', o.maxHistory));
        return el;
      }
    },

    socket: {
      label: 'Socket',
      className: 'ch.qos.logback.classic.net.SocketAppender',
      defaults: { remoteHost: '192.168.1.5', port: '4560', reconnectionDelay: '10000', includeCallerData: false },
      fields: [
        { key: 'remoteHost', label: 'Remote host', type: 'text' },
        { key: 'port', label: 'Port', type: 'text', size: 'small' },
        { key: 'reconnectionDelay', label: 'Reconnection delay (ms)', type: 'text', size: 'small' },
        { key: 'includeCallerData', label: 'Include caller data', type: 'bool' }
      ],
      body: function (o) {
        var el = [tag('remoteHost', o.remoteHost), tag('port', o.port)];
        if (o.reconnectionDelay) el.push(tag('reconnectionDelay', o.reconnectionDelay));
        if (o.includeCallerData) el.push(tag('includeCallerData', 'true'));
        return el;
      }
    },

    syslog: {
      label: 'Syslog',
      className: 'ch.qos.logback.classic.net.SyslogAppender',
      defaults: { syslogHost: '192.168.1.5', port: '514', facility: 'USER', suffixPattern: '[%thread] %logger %msg' },
      fields: [
        { key: 'syslogHost', label: 'Syslog host', type: 'text' },
        { key: 'port', label: 'Port', type: 'text', size: 'small' },
        { key: 'facility', label: 'Facility', type: 'select', options: SYSLOG_FACILITIES },
        { key: 'suffixPattern', label: 'Suffix pattern', type: 'text' }
      ],
      body: function (o) {
        return [
          tag('syslogHost', o.syslogHost),
          tag('port', o.port),
          tag('facility', o.facility),
          tag('suffixPattern', o.suffixPattern)
        ];
      }
    },

    smtp: {
      label: 'Email (SMTP)',
      className: 'ch.qos.logback.classic.net.SMTPAppender',
      defaults: {
        smtpHost: 'smtp.example.com', smtpPort: '25', ssl: false,
        username: '', password: '',
        to: 'errors@example.com', from: 'app@example.com',
        subject: '%logger{20} - %m',
        pattern: '%date %-5level %logger{35} - %message%n'
      },
      fields: [
        { key: 'smtpHost', label: 'SMTP host', type: 'text' },
        { key: 'smtpPort', label: 'SMTP port', type: 'text', size: 'small' },
        { key: 'ssl', label: 'Use SSL', type: 'bool' },
        { key: 'username', label: 'Username (optional)', type: 'text' },
        { key: 'password', label: 'Password (optional)', type: 'text' },
        { key: 'to', label: 'To', type: 'text' },
        { key: 'from', label: 'From', type: 'text' },
        { key: 'subject', label: 'Subject pattern', type: 'text' },
        { key: 'pattern', label: 'Message pattern', type: 'text' }
      ],
      body: function (o) {
        var el = [tag('smtpHost', o.smtpHost), tag('smtpPort', o.smtpPort)];
        if (o.ssl) el.push(tag('SSL', 'true'));
        if (o.username) el.push(tag('username', o.username));
        if (o.password) el.push(tag('password', o.password));
        el.push(tag('to', o.to), tag('from', o.from), tag('subject', o.subject));
        el.push(wrap('layout', [tag('pattern', o.pattern)], { class: 'ch.qos.logback.classic.PatternLayout' }));
        return el;
      }
    }
  };

  // ---------------------------------------------------------------------
  // State
  // ---------------------------------------------------------------------

  var nextId = 1;

  function defaultState() {
    return {
      debug: false,
      properties: [],
      rootLevel: 'DEBUG',
      appenders: [],
      loggers: []
    };
  }

  var state = null;

  var PRESETS = {
    logcat: function () {
      var s = defaultState();
      s.appenders.push(makeAppenderFor(s, 'logcat'));
      return s;
    },
    'logcat-file': function () {
      var s = defaultState();
      s.appenders.push(makeAppenderFor(s, 'logcat'));
      s.appenders.push(makeAppenderFor(s, 'rolling'));
      s.loggers.push({ name: 'com.example', level: 'DEBUG', additivity: true });
      return s;
    },
    remote: function () {
      var s = defaultState();
      s.appenders.push(makeAppenderFor(s, 'logcat'));
      var sock = makeAppenderFor(s, 'socket');
      sock.filterLevel = 'WARN';
      s.appenders.push(sock);
      return s;
    }
  };

  // makeAppender variant usable before `state` points at the target object
  function makeAppenderFor(s, type) {
    var def = APPENDER_TYPES[type];
    var names = s.appenders.map(function (a) { return a.name; });
    var name = type, n = 2;
    while (names.indexOf(name) !== -1) name = type + '-' + (n++);
    return {
      id: nextId++,
      type: type,
      name: name,
      attachToRoot: true,
      filterLevel: '',
      options: JSON.parse(JSON.stringify(def.defaults))
    };
  }

  function saveState() {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch (e) { /* ignore */ }
  }

  function loadState() {
    try {
      var raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      var s = JSON.parse(raw);
      if (!s || !Array.isArray(s.appenders) || !Array.isArray(s.loggers)) return null;
      // re-seed id counter past any stored ids
      s.appenders.forEach(function (a) { if (a.id >= nextId) nextId = a.id + 1; });
      return s;
    } catch (e) {
      return null;
    }
  }

  // ---------------------------------------------------------------------
  // XML generation
  // ---------------------------------------------------------------------

  function escapeXml(s) {
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  // element helpers produce {name, attrs, children|text}
  function tag(name, text) { return { name: name, text: text == null ? '' : String(text) }; }
  function wrap(name, children, attrs) { return { name: name, attrs: attrs || {}, children: children }; }

  function renderEl(el, indent) {
    var pad = new Array(indent + 1).join('  ');
    var attrs = '';
    if (el.attrs) {
      for (var k in el.attrs) {
        if (el.attrs[k] !== undefined && el.attrs[k] !== '') {
          attrs += ' ' + k + '="' + escapeXml(el.attrs[k]) + '"';
        }
      }
    }
    if (el.children && el.children.length) {
      var inner = el.children.map(function (c) { return renderEl(c, indent + 1); }).join('\n');
      return pad + '<' + el.name + attrs + '>\n' + inner + '\n' + pad + '</' + el.name + '>';
    }
    if (el.text !== undefined && el.text !== '') {
      return pad + '<' + el.name + attrs + '>' + escapeXml(el.text) + '</' + el.name + '>';
    }
    return pad + '<' + el.name + attrs + ' />';
  }

  function generateXml() {
    var lines = [];
    var attrs = 'xmlns="https://tony19.github.io/logback-android/xml"\n' +
      '  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n' +
      '  xsi:schemaLocation="https://tony19.github.io/logback-android/xml https://cdn.jsdelivr.net/gh/tony19/logback-android/logback.xsd"';
    if (state.debug) attrs += '\n  debug="true"';

    lines.push('<configuration');
    lines.push('  ' + attrs);
    lines.push('>');

    var blocks = [];

    if (state.properties.length) {
      blocks.push(state.properties
        .filter(function (p) { return p.key; })
        .map(function (p) {
          return '  <property name="' + escapeXml(p.key) + '" value="' + escapeXml(p.value) + '" />';
        }).join('\n'));
    }

    state.appenders.forEach(function (a) {
      var def = APPENDER_TYPES[a.type];
      var children = [];
      if (a.filterLevel) {
        children.push(wrap('filter', [tag('level', a.filterLevel)],
          { class: 'ch.qos.logback.classic.filter.ThresholdFilter' }));
      }
      children = children.concat(def.body(a.options));
      blocks.push(renderEl(wrap('appender', children, { name: a.name, class: def.className }), 1));
    });

    state.loggers.forEach(function (l) {
      if (!l.name) return;
      var attrsStr = ' name="' + escapeXml(l.name) + '"';
      if (l.level) attrsStr += ' level="' + escapeXml(l.level) + '"';
      if (l.additivity === false) attrsStr += ' additivity="false"';
      blocks.push('  <logger' + attrsStr + ' />');
    });

    var rootChildren = state.appenders
      .filter(function (a) { return a.attachToRoot; })
      .map(function (a) { return '    <appender-ref ref="' + escapeXml(a.name) + '" />'; });
    var rootBlock = '  <root level="' + escapeXml(state.rootLevel) + '">';
    if (rootChildren.length) {
      rootBlock += '\n' + rootChildren.join('\n') + '\n  </root>';
    } else {
      rootBlock += '\n  </root>';
    }
    blocks.push(rootBlock);

    lines.push(blocks.join('\n\n'));
    lines.push('</configuration>');
    return lines.join('\n');
  }

  // ---------------------------------------------------------------------
  // XML syntax highlighting (tokenizes; single-pass replaces, no rescans)
  // ---------------------------------------------------------------------

  function escapeHtml(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  function highlightXml(xml) {
    var out = '';
    var tokens = xml.match(/<!--[\s\S]*?-->|<[^>]+>|[^<]+/g) || [];
    tokens.forEach(function (t) {
      if (t.lastIndexOf('<!--', 0) === 0) {
        out += '<span class="xml-comment">' + escapeHtml(t) + '</span>';
      } else if (t.charAt(0) === '<') {
        var m = t.match(/^<(\/?)([\w.:-]+)([\s\S]*?)(\/?)>$/);
        if (m) {
          var attrs = escapeHtml(m[3]).replace(/([\w.:-]+)=(&quot;|")([^"]*)(&quot;|")/g,
            '<span class="attr-name">$1</span>="<span class="attr-value">$3</span>"');
          out += '&lt;' + m[1] + '<span class="tag-name">' + m[2] + '</span>' + attrs + m[4] + '&gt;';
        } else {
          out += escapeHtml(t);
        }
      } else {
        out += '<span class="xml-text">' + escapeHtml(t) + '</span>';
      }
    });
    return out;
  }

  // ---------------------------------------------------------------------
  // Form rendering
  // ---------------------------------------------------------------------

  function h(html) {
    var t = document.createElement('template');
    t.innerHTML = html.trim();
    return t.content.firstChild;
  }

  function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  function levelSelect(value, includeInherit) {
    var opts = includeInherit ? ['', 'TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'] : LEVELS;
    return opts.map(function (l) {
      var label = l === '' ? '(inherited)' : l;
      return '<option value="' + l + '"' + (l === value ? ' selected' : '') + '>' + label + '</option>';
    }).join('');
  }

  function renderSettings() {
    var host = document.getElementById('settings-body');
    var propRows = state.properties.map(function (p, i) {
      return '<div class="form-row" data-prop-row="' + i + '">' +
        '<div class="field"><label>Name</label>' +
        '<input type="text" data-prop="' + i + '" data-prop-key="key" value="' + esc(p.key) + '" placeholder="LOG_DIR_NAME"></div>' +
        '<div class="field"><label>Value</label>' +
        '<input type="text" data-prop="' + i + '" data-prop-key="value" value="' + esc(p.value) + '" placeholder="logs"></div>' +
        '<div class="field" style="justify-content: flex-end;"><label>&nbsp;</label>' +
        '<button type="button" class="btn btn-danger-ghost btn-sm" data-remove-prop="' + i + '">Remove</button></div>' +
        '</div>';
    }).join('');

    host.innerHTML =
      '<div class="form-row">' +
      '<div class="field"><label>Root logger level</label>' +
      '<select id="root-level">' + levelSelect(state.rootLevel, false) + '</select></div>' +
      '<div class="field"><label>Diagnostics</label>' +
      '<label class="check"><input type="checkbox" id="cfg-debug"' + (state.debug ? ' checked' : '') + '> Print logback status (debug="true")</label></div>' +
      '</div>' +
      '<div style="margin-top: 0.75rem;">' +
      '<label style="font-size: 0.78rem; font-weight: 600; color: var(--text-muted);">Properties (usable as ${name} in values below)</label>' +
      propRows +
      '<button type="button" class="btn btn-ghost btn-sm" id="add-prop" style="margin-top: 0.5rem;">+ Add property</button>' +
      '</div>';
  }

  function renderAppenders() {
    var host = document.getElementById('appenders-body');
    if (!state.appenders.length) {
      host.innerHTML = '<div class="empty-hint">No appenders yet — add one below. Without at least one appender, nothing is logged.</div>';
      return;
    }
    host.innerHTML = state.appenders.map(function (a) {
      var def = APPENDER_TYPES[a.type];
      var fieldHtml = def.fields.map(function (f) {
        var v = a.options[f.key];
        if (f.type === 'bool') {
          return '<div class="field"><label>&nbsp;</label>' +
            '<label class="check"><input type="checkbox" data-app="' + a.id + '" data-opt="' + f.key + '"' +
            (v ? ' checked' : '') + '> ' + esc(f.label) + '</label></div>';
        }
        if (f.type === 'select') {
          var opts = f.options.map(function (o) {
            return '<option value="' + esc(o) + '"' + (o === v ? ' selected' : '') + '>' + esc(o) + '</option>';
          }).join('');
          return '<div class="field"><label>' + esc(f.label) + '</label>' +
            '<select data-app="' + a.id + '" data-opt="' + f.key + '">' + opts + '</select></div>';
        }
        var wide = (String(v).length > 30 || f.key === 'pattern' || f.key === 'fileNamePattern') ? ' wide' : '';
        return '<div class="field' + wide + '"><label>' + esc(f.label) + '</label>' +
          '<input type="text" class="mono" data-app="' + a.id + '" data-opt="' + f.key + '" value="' + esc(v == null ? '' : v) + '"></div>';
      }).join('');

      return '<div class="fieldset" data-appender-box="' + a.id + '">' +
        '<div class="fieldset-header">' +
        '<span class="tag">' + esc(def.label) + '</span>' +
        '<h4 class="mono">' + esc(a.name) + '</h4>' +
        '<button type="button" class="btn btn-danger-ghost btn-sm remove" data-remove-app="' + a.id + '">Remove</button>' +
        '</div>' +
        '<div class="form-row">' +
        '<div class="field"><label>Appender name</label>' +
        '<input type="text" class="mono" data-app="' + a.id + '" data-meta="name" value="' + esc(a.name) + '"></div>' +
        '<div class="field"><label>Threshold filter (min level)</label>' +
        '<select data-app="' + a.id + '" data-meta="filterLevel">' +
        FILTER_LEVELS.map(function (l) {
          var label = l === '' ? '(none — log everything)' : l + ' and above';
          return '<option value="' + l + '"' + (l === a.filterLevel ? ' selected' : '') + '>' + label + '</option>';
        }).join('') +
        '</select></div>' +
        '<div class="field"><label>&nbsp;</label>' +
        '<label class="check"><input type="checkbox" data-app="' + a.id + '" data-meta="attachToRoot"' +
        (a.attachToRoot ? ' checked' : '') + '> Attach to root logger</label></div>' +
        '</div>' +
        '<div class="form-row">' + fieldHtml + '</div>' +
        '</div>';
    }).join('');
  }

  function renderLoggers() {
    var host = document.getElementById('loggers-body');
    if (!state.loggers.length) {
      host.innerHTML = '<div class="empty-hint">No custom loggers — every class inherits the root level. Add a logger to raise or lower the level for a specific package.</div>';
      return;
    }
    host.innerHTML = state.loggers.map(function (l, i) {
      return '<div class="fieldset" data-logger-box="' + i + '">' +
        '<div class="form-row">' +
        '<div class="field wide" style="grid-column: span 2;"><label>Logger name (package or class)</label>' +
        '<input type="text" class="mono" data-logger="' + i + '" data-lkey="name" value="' + esc(l.name) + '" placeholder="com.example.myapp"></div>' +
        '<div class="field"><label>Level</label>' +
        '<select data-logger="' + i + '" data-lkey="level">' + levelSelect(l.level, true) + '</select></div>' +
        '<div class="field"><label>&nbsp;</label>' +
        '<label class="check"><input type="checkbox" data-logger="' + i + '" data-lkey="additivity"' +
        (l.additivity !== false ? ' checked' : '') + '> Additive (pass to root)</label></div>' +
        '<div class="field" style="justify-content: flex-end;"><label>&nbsp;</label>' +
        '<button type="button" class="btn btn-danger-ghost btn-sm" data-remove-logger="' + i + '">Remove</button></div>' +
        '</div>' +
        '</div>';
    }).join('');
  }

  function renderXml() {
    var xml = generateXml();
    document.getElementById('xml-out').innerHTML = highlightXml(xml);
    return xml;
  }

  function renderAll() {
    renderSettings();
    renderAppenders();
    renderLoggers();
    renderXml();
    saveState();
  }

  function refresh() {
    renderXml();
    saveState();
  }

  // ---------------------------------------------------------------------
  // Event wiring (delegation so typing never loses focus)
  // ---------------------------------------------------------------------

  function findAppender(id) {
    for (var i = 0; i < state.appenders.length; i++) {
      if (state.appenders[i].id === Number(id)) return state.appenders[i];
    }
    return null;
  }

  function onFormInput(e) {
    var t = e.target;

    if (t.id === 'root-level') { state.rootLevel = t.value; return refresh(); }
    if (t.id === 'cfg-debug') { state.debug = t.checked; return refresh(); }

    if (t.hasAttribute('data-prop')) {
      var p = state.properties[Number(t.getAttribute('data-prop'))];
      if (p) p[t.getAttribute('data-prop-key')] = t.value;
      return refresh();
    }

    if (t.hasAttribute('data-app')) {
      var a = findAppender(t.getAttribute('data-app'));
      if (!a) return;
      var meta = t.getAttribute('data-meta');
      if (meta) {
        if (meta === 'attachToRoot') a.attachToRoot = t.checked;
        else a[meta] = t.value;
        if (meta === 'name') {
          var box = document.querySelector('[data-appender-box="' + a.id + '"] h4');
          if (box) box.textContent = a.name;
        }
      } else {
        var opt = t.getAttribute('data-opt');
        a.options[opt] = (t.type === 'checkbox') ? t.checked : t.value;
      }
      return refresh();
    }

    if (t.hasAttribute('data-logger')) {
      var l = state.loggers[Number(t.getAttribute('data-logger'))];
      if (!l) return;
      var k = t.getAttribute('data-lkey');
      l[k] = (t.type === 'checkbox') ? t.checked : t.value;
      return refresh();
    }
  }

  function onFormClick(e) {
    var t = e.target.closest('button');
    if (!t) return;

    if (t.id === 'add-prop') {
      state.properties.push({ key: '', value: '' });
      renderSettings(); refresh();
      return;
    }
    if (t.hasAttribute('data-remove-prop')) {
      state.properties.splice(Number(t.getAttribute('data-remove-prop')), 1);
      renderSettings(); refresh();
      return;
    }
    if (t.hasAttribute('data-remove-app')) {
      var id = Number(t.getAttribute('data-remove-app'));
      state.appenders = state.appenders.filter(function (a) { return a.id !== id; });
      renderAppenders(); refresh();
      return;
    }
    if (t.hasAttribute('data-add-appender')) {
      state.appenders.push(makeAppenderFor(state, t.getAttribute('data-add-appender')));
      renderAppenders(); refresh();
      return;
    }
    if (t.id === 'add-logger') {
      state.loggers.push({ name: '', level: 'DEBUG', additivity: true });
      renderLoggers(); refresh();
      var inputs = document.querySelectorAll('[data-lkey="name"]');
      if (inputs.length) inputs[inputs.length - 1].focus();
      return;
    }
    if (t.hasAttribute('data-remove-logger')) {
      state.loggers.splice(Number(t.getAttribute('data-remove-logger')), 1);
      renderLoggers(); refresh();
      return;
    }
  }

  // ---------------------------------------------------------------------
  // Toolbar: presets, copy, download, reset
  // ---------------------------------------------------------------------

  function initToolbar() {
    document.getElementById('preset-select').addEventListener('change', function (e) {
      var key = e.target.value;
      if (key && PRESETS[key]) {
        state = PRESETS[key]();
        renderAll();
      }
      e.target.value = '';
    });

    document.getElementById('copy-xml').addEventListener('click', function () {
      var btn = this;
      navigator.clipboard.writeText(generateXml()).then(function () {
        var orig = btn.textContent;
        btn.textContent = 'Copied!';
        setTimeout(function () { btn.textContent = orig; }, 1500);
      });
    });

    document.getElementById('download-xml').addEventListener('click', function () {
      var blob = new Blob([generateXml() + '\n'], { type: 'application/xml' });
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      a.href = url;
      a.download = 'logback.xml';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
    });

    document.getElementById('reset-editor').addEventListener('click', function () {
      if (!window.confirm('Reset the editor to the default configuration? This discards your current config.')) return;
      try { localStorage.removeItem(STORAGE_KEY); } catch (e) { /* ignore */ }
      state = PRESETS.logcat();
      renderAll();
    });
  }

  // ---------------------------------------------------------------------
  // Boot
  // ---------------------------------------------------------------------

  document.addEventListener('DOMContentLoaded', function () {
    state = loadState() || PRESETS.logcat();

    var form = document.getElementById('editor-form');
    form.addEventListener('input', onFormInput);
    form.addEventListener('change', onFormInput);
    form.addEventListener('click', onFormClick);

    initToolbar();
    renderAll();
  });
})();
