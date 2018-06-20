package ru.majordomo.hms.personmgr.service.restic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;

@EqualsAndHashCode(callSuper = false)
@Data
public class SnapshotsResponse extends ArrayList<Snapshot> {}
