// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package queries.schemas

import explore.model.ResizableSection
import lucuma.core.model.User

final case class WidthUpsertInput(user: User.Id, section: ResizableSection, width: Int)
