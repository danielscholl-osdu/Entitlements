package org.opengroup.osdu.entitlements.v2.model.init;

import java.util.List;
import lombok.*;

@Data
@Builder
@Generated
@NoArgsConstructor
@AllArgsConstructor
public class InitServiceDto {
  List<AliasEntity> aliasMappings;
}
