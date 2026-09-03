import 'package:flutter/material.dart';

import 'experience_form_page.dart';

class MonetizedExperienceFormPage extends StatelessWidget {
  const MonetizedExperienceFormPage({super.key, this.id, this.upgradeSourceId});

  final int? id;
  final int? upgradeSourceId;

  @override
  Widget build(BuildContext context) => ExperienceFormPage(
    id: id,
    upgradeSourceId: upgradeSourceId,
    businessType: ExperienceBusinessType.monetized,
  );
}
