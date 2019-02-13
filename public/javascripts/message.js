$(function() {
  $root = $('main.message');
  $root.find('select.select').change(function() {
    $root.find('input[name=threads]').prop('checked', false);
    switch ($(this).val()) {
      case 'all':
        $root.find('input[name=threads]').prop('checked', true);
        break;
      case 'read':
        $root.find('tr:not(.new) input[name=threads]').prop('checked', true);
        break;
      case 'unread':
        $root.find('tr.new input[name=threads]').prop('checked', true);
        break;
      case 'study':
        $root.find('tr.new input[name=threads]').prop('checked', true);
        break;
    }
  });
  $root.find('select.action').change(function() {
    var action = $(this).val();
    if (!action) return;
    var ids = [];
    $root.find('input[name=threads]:checked').each(function() {
      return ids.push(this.value);
    });
    if (ids.length === 0) return;
    if (action === 'delete' && !confirm('Delete ' + ids.length + ' message(s)?')) return;
    var url = '/inbox/batch?action=' + action + '&ids=' + ids.join(',');
    var $form = $('<form method="post">').attr('action', url);
    $root.prepend($form);
    $form.submit();
  });

  var presets = window.lidraughts_mod_presets;
  if (presets) {

    var toggle = $root.find('input[name=mod]');
    var select = $root.find('select[name=preset]');
    presets.forEach(function(p, i) {
      select.append('<option value=' + i + '>' + p[0] + '</option>');
    });
    select.on('change', function() {
      var p = presets[$(this).val()] || ['', ''];
      $root.find('input[name=subject]').val(p[0]);
      $root.find('textarea[name=text]').val(p[1]);
    });

    var toggleSelect = function() {
      select.parent().toggle(toggle.prop('checked'));
    };
    toggleSelect();
    toggle.on('change', toggleSelect);
  }
});
