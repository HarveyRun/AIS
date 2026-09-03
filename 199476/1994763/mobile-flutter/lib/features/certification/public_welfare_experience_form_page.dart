import 'package:flutter/material.dart';

import 'experience_form_page.dart';

class PublicWelfareExperienceFormPage extends StatelessWidget {
  const PublicWelfareExperienceFormPage({super.key, this.id});

  final int? id;

  @override
  Widget build(BuildContext context) => ExperienceFormPage(
    id: id,
    businessType: ExperienceBusinessType.publicWelfare,
  );
}
