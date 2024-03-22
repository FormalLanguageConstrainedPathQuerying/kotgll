package org.srcgll.input

class RecoveryLinearInput<VertexType, LabelType : ILabel>
    : LinearInput<VertexType, LabelType>(), IRecoveryInputGraph<VertexType, LabelType>